package uk.gov.moj.cpp.staging.prosecutors.service;

import static java.util.UUID.randomUUID;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.ResultCode;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class SystemIdMapperService {

    private static final String CASE_CREATION_TARGET_TYPE = "CASE_CREATION_SUBMISSION_ID";

    private static final String CASE_CREATION_SOURCE_TYPE = "CASE_CREATION_URN";

    private static final String SPI_SOURCE_TYPE = "OU_URN";

    private static final String SPI_TARGET_TYPE = "CASE_FILE_ID";

    private static final String SOURCE_TYPE = "BS_DOCUMENT_REFERENCE";

    private static final String TARGET_TYPE = "DOCUMENT_SUBMISSION_ID";

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private SystemIdMapperClient systemIdMapperClient;

    public Pair<UUID, Boolean> getSubmissionIdForUrnWithMatchFound(final String prosecutorCaseReference) {
        final Optional<SystemIdMapping> mapping = getSystemIdMappingForCaseUrnRelatedToCaseCreation(prosecutorCaseReference);
        if (mapping.isPresent()) {
            return new ImmutablePair<>(mapping.get().getTargetId(), Boolean.TRUE);
        }

        // submission id
        final UUID newSubmissionId = randomUUID();
        attemptAddMappingForSubmissionIdAndURN(newSubmissionId, prosecutorCaseReference);
        return new ImmutablePair<>(newSubmissionId, Boolean.FALSE);

    }

    public UUID getCppCaseIdForPtiUrn(final String prosecutorCaseReference) {
        final Optional<SystemIdMapping> mappingForPtiUrn = getSystemIdMappingForSpiCase(prosecutorCaseReference);
        if (mappingForPtiUrn.isPresent()) {
            return mappingForPtiUrn.get().getTargetId();
        }

        final UUID newCaseId = randomUUID();
        final AdditionResponse additionResponse = attemptAddMappingForURN(newCaseId, prosecutorCaseReference);
        if (additionResponse.isSuccess()) {
            return newCaseId;
        }

        return getSystemIdMappingForSpiCase(prosecutorCaseReference)
                .orElseThrow(() -> new IllegalStateException("Error generating SPI case id"))
                .getTargetId();
    }

    public AdditionResponse attemptAddMappingForSrcDocumentReference(final String srcDocumentReference, final UUID submissionId) {
        final SystemIdMap systemIdMap = new SystemIdMap(srcDocumentReference, SOURCE_TYPE, submissionId, TARGET_TYPE);

        final Optional<UUID> contextSystemUserId = systemUserProvider.getContextSystemUserId();

        if (contextSystemUserId.isPresent()) {
            return systemIdMapperClient.add(systemIdMap, contextSystemUserId.get());
        }

        return new AdditionResponse(submissionId, ResultCode.CONFLICT, Optional.of("system id mapper service failed to add document Reference mapping"));
    }

    //This mapping is introduced to handle cases created through SPI flow as they have different source and target names defined in the systemIdMapper.
    //To do Solution will be given by architects.
    private Optional<SystemIdMapping> getSystemIdMappingForSpiCase(final String prosecutorCaseReference) {
        final Optional<UUID> contextSystemUserId = systemUserProvider.getContextSystemUserId();
        if (contextSystemUserId.isPresent()) {

            return systemIdMapperClient.findBy(prosecutorCaseReference, SPI_SOURCE_TYPE, SPI_TARGET_TYPE, contextSystemUserId.get());
        }
        return Optional.empty();
    }

    private AdditionResponse attemptAddMappingForURN(final UUID caseId, final String prosecutorCaseReference) {
        final SystemIdMap systemIdMap = new SystemIdMap(prosecutorCaseReference, SPI_SOURCE_TYPE, caseId, SPI_TARGET_TYPE);

        final Optional<UUID> contextSystemUserId = systemUserProvider.getContextSystemUserId();

        if (contextSystemUserId.isPresent()) {
            return systemIdMapperClient.add(systemIdMap, contextSystemUserId.get());
        }

        return new AdditionResponse(caseId, ResultCode.CONFLICT, Optional.of("system id mapper service failed to add urn mapping"));
    }

    private AdditionResponse attemptAddMappingForSubmissionIdAndURN(final UUID submissionId, final String prosecutorCaseReference) {
        final SystemIdMap systemIdMap = new SystemIdMap(prosecutorCaseReference, CASE_CREATION_SOURCE_TYPE, submissionId, CASE_CREATION_TARGET_TYPE);

        final Optional<UUID> contextSystemUserId = systemUserProvider.getContextSystemUserId();

        if (contextSystemUserId.isPresent()) {
            return systemIdMapperClient.add(systemIdMap, contextSystemUserId.get());
        }

        return new AdditionResponse(submissionId, ResultCode.CONFLICT, Optional.of("system id mapper service failed to add urn mapping"));
    }

    private Optional<SystemIdMapping> getSystemIdMappingForCaseUrnRelatedToCaseCreation(final String prosecutorCaseReference) {
        final Optional<UUID> contextSystemUserId = systemUserProvider.getContextSystemUserId();
        if (contextSystemUserId.isPresent()) {

            return systemIdMapperClient.findBy(prosecutorCaseReference, CASE_CREATION_SOURCE_TYPE, CASE_CREATION_TARGET_TYPE, contextSystemUserId.get());
        }
        return Optional.empty();
    }

}
