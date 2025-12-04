package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.util.UUID.randomUUID;

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

    private static final String SOURCE_TYPE = "OU_URN";

    private static final String TARGET_TYPE = "CASE_FILE_ID";

    private static final String SPI_SOURCE_TYPE = "SPI-URN";

    private static final String SPI_TARGET_TYPE = "CASE-ID";

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private SystemIdMapperClient systemIdMapperClient;

    public UUID getCppCaseIdFor(final String prosecutorCaseReference) {
        final Optional<SystemIdMapping> mapping = getSystemIdMappingFor(prosecutorCaseReference);
        if (mapping.isPresent()) {
            return mapping.get().getTargetId();
        }

        final Optional<SystemIdMapping> mappingForPtiUrn = getSystemIdMappingForSpiCase(prosecutorCaseReference);
        if (mappingForPtiUrn.isPresent()) {
            return mappingForPtiUrn.get().getTargetId();
        }

        if (prosecutorCaseReference.contains(":")) {
            final Optional<SystemIdMapping> mappingForCaseReference = getSystemIdMappingFor(prosecutorCaseReference.substring(prosecutorCaseReference.lastIndexOf(':') + 1));
            if (mappingForCaseReference.isPresent()) {
                return mappingForCaseReference.get().getTargetId();
            }
        }

        final UUID newCaseId = randomUUID();
        final AdditionResponse additionResponse = attemptAddMappingForURN(newCaseId, prosecutorCaseReference);
        final AdditionResponse additionResponse2 = attemptAddMappingForURN(newCaseId, prosecutorCaseReference.substring(prosecutorCaseReference.lastIndexOf(':')+1));
        if (additionResponse.isSuccess() && additionResponse2.isSuccess()) {
            return newCaseId;
        }

        return getSystemIdMappingFor(prosecutorCaseReference)
                .orElseThrow(() -> new IllegalStateException("Error generating case id"))
                .getTargetId();
    }

    public UUID getCaseIdForMaterialSubmission(final String prosecutorCaseReference){
        final Optional<SystemIdMapping> mapping = getSystemIdMappingFor(prosecutorCaseReference);
        if (mapping.isPresent()) {
            return mapping.get().getTargetId();
        }

        final Optional<SystemIdMapping> mappingUrn = getSystemIdMappingFor(prosecutorCaseReference.substring(prosecutorCaseReference.lastIndexOf(':')+1));
        if (mappingUrn.isPresent()) {
            return mappingUrn.get().getTargetId();
        }

        final UUID newCaseId = randomUUID();
        final AdditionResponse additionResponse1 = attemptAddMappingForURN(newCaseId, prosecutorCaseReference);
        final AdditionResponse additionResponse2 = attemptAddMappingForURN(newCaseId, prosecutorCaseReference.substring(prosecutorCaseReference.lastIndexOf(':')+1));

        if (additionResponse1.isSuccess() && additionResponse2.isSuccess()) {
            return newCaseId;
        }

        return getSystemIdMappingFor(prosecutorCaseReference)
                .orElseThrow(() -> new IllegalStateException("Error generating case id"))
                .getTargetId();
    }

    private Optional<SystemIdMapping> getSystemIdMappingFor(final String prosecutorCaseReference) {
        final Optional<UUID> contextSystemUserId = systemUserProvider.getContextSystemUserId();
        if (contextSystemUserId.isPresent()) {
            return systemIdMapperClient.findBy(prosecutorCaseReference, SOURCE_TYPE, TARGET_TYPE, contextSystemUserId.get());
        }
        return Optional.empty();
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
        final SystemIdMap systemIdMap = new SystemIdMap(prosecutorCaseReference, SOURCE_TYPE, caseId, TARGET_TYPE);
        final Optional<UUID> contextSystemUserId = systemUserProvider.getContextSystemUserId();
        if (contextSystemUserId.isPresent()) {
            return systemIdMapperClient.add(systemIdMap, contextSystemUserId.get());
        }
        return new AdditionResponse(caseId, ResultCode.CONFLICT, Optional.of("system id mapper service failed to add urn mapping"));
    }
}