package uk.gov.moj.cpp.staging.prosecutors;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitChargeProsecutionWithSubmissionId.submitChargeProsecutionWithSubmissionId;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitRequisitionProsecutionWithSubmissionId.submitRequisitionProsecutionWithSubmissionId;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitSummonsProsecutionWithSubmissionId.submitSummonsProsecutionWithSubmissionId;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttpV2.submitSjpProsecutionHttpV2;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutorapi.query.view.SubmissionQueryView;
import uk.gov.moj.cpp.staging.prosecutors.command.api.ChargeDefendant;
import uk.gov.moj.cpp.staging.prosecutors.command.api.ChargeOffence;
import uk.gov.moj.cpp.staging.prosecutors.command.api.RequisitionDefendant;
import uk.gov.moj.cpp.staging.prosecutors.command.api.RequisitionOffence;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitChargeProsecutionHttpWithOucode;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitChargeProsecutionWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitRequisitionProsecutionHttpWithOucode;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitRequisitionProsecutionWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitSummonsProsecutionHttpWithOucode;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitSummonsProsecutionWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SummonsDefendant;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SummonsOffence;
import uk.gov.moj.cpp.staging.prosecutors.converter.SubmitSjpProsecutionV2Converter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttpV2;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttpWithOucode;
import uk.gov.moj.cpp.staging.prosecutors.pojo.SubmitSjpProsecution;
import uk.gov.moj.cpp.staging.prosecutors.service.SystemIdMapperService;
import uk.gov.moj.cpp.staging.prosecutors.uuid.UUIDProducer;
import uk.gov.moj.cpp.staging.prosecutors.validators.DefendantValidator;
import uk.gov.moj.cpp.staging.prosecutors.validators.OffenceValidator;
import uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionHttpV2Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_API)
public class StagingProsecutorsCommandAPIV2 {

    @Inject
    @Value(key = "stagingprosecutors.submit-prosecution-response.base-url", defaultValue = "https://replace-me.gov.uk/")
    String baseResponseURL;

    private static final String RESPONSE_URL_VERSION_PLACEHOLDER = "VERSION";
    private static final String VERSION_NO = "v2";
    private static final String SUBMISSION_ID = "submissionId";
    private static final String OUCODE= "oucode";

    @Inject
    private Sender sender;

    @Inject
    private SubmitSjpProsecutionV2Converter submitSjpProsecutionV2Converter;

    @Inject
    private SubmitSjpProsecutionHttpV2Validator submitSjpProsecutionHttpV2Validator;

    @Inject
    private OffenceValidator offenceValidator;

    @Inject
    private DefendantValidator defendantValidator;

    @Inject
    private UUIDProducer uuidProducer;

    @Inject
    private JsonSchemaValidator jsonSchemaValidator;


    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Inject
    private SubmissionQueryView submissionQueryView;

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingProsecutorsCommandAPIV2.class);

    private final ObjectMapper mapper = new ObjectMapperProducer().objectMapper().copy();

    private static final String PROSECUTING_AUTHORITY_FIELD = "prosecutingAuthority";
    private static final String SRC_DOCUMENT_REFERENCE_FIELD = "srcDocumentReference";
    private static final String CASE_URN = "caseUrn";
    private static final String MATERIAL_TYPE = "materialType";
    private static final String STATUS = "status";

    @Handles("hmcts.cjs.summons-prosecution.v2")
    public Envelope<UrlResponse> submitSummonsProsecution(final Envelope<SubmitSummonsProsecutionHttpWithOucode> envelope) {
        final SubmitSummonsProsecutionHttpWithOucode payload = envelope.payload();

        if (!StringUtils.equalsIgnoreCase(payload.getOucode(), payload.getProsecutionSubmissionDetails().getProsecutingAuthority())) {
            throw new ForbiddenRequestException("");
        }


        final Map<String, List<String>> violations = new HashMap<>();

        final List<DefendantDetails> defendantDetails = payload
                .getDefendants()
                .stream()
                .map(SummonsDefendant::getDefendantDetails)
                .filter(Objects::nonNull)
                .map(Optional::ofNullable)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        defendantValidator.validate(defendantDetails, violations);

        payload.getDefendants().forEach(chargeDefendant ->
                offenceValidator
                        .validate(chargeDefendant.getOffences()
                                .stream()
                                .map(SummonsOffence::getOffenceDetails)
                                .collect(Collectors.toList()), violations));

        if (violations.size() > 0) {
            throwBadRequestException(violations);
        }

        final String urn = payload.getProsecutionSubmissionDetails().getUrn();
        final Pair<UUID, Boolean> submissionIdWithMatchFoundPair = systemIdMapperService.getSubmissionIdForUrnWithMatchFound(urn);

        final JsonObject submissionPayload = createObjectBuilder()
                .add(SUBMISSION_ID, submissionIdWithMatchFoundPair.getLeft().toString())
                .add(OUCODE, envelope.payload().getOucode())
                .build();

        final JsonObject submissionResponse = submissionQueryView.querySubmissionV2(submissionPayload);

        if (!submissionIdWithMatchFoundPair.getRight() || shouldRaiseSubmission(submissionResponse)) {
            final SubmitSummonsProsecutionWithSubmissionId payloadWithSubmissionId =
                    submitSummonsProsecutionWithSubmissionId()
                            .withDefendants(payload.getDefendants())
                            .withProsecutionSubmissionDetails(payload.getProsecutionSubmissionDetails())
                            .withSubmissionId( submissionIdWithMatchFoundPair.getLeft())
                            .build();

            sender.send(envelop(payloadWithSubmissionId)
                    .withName("stagingprosecutors.command.summons-prosecution")
                    .withMetadataFrom(envelope));

        }
        final UUID submissionId = submissionIdWithMatchFoundPair.getLeft();

        return Envelope.envelopeFrom(envelope.metadata(), new UrlResponse(getBaseResponseURLWithVersion() + submissionId.toString(), submissionId));
    }

    @Handles("hmcts.cjs.charge-prosecution.v2")
    public Envelope<UrlResponse> submitChargeProsecution(final Envelope<SubmitChargeProsecutionHttpWithOucode> envelope) {
        if (!StringUtils.equalsIgnoreCase(envelope.payload().getOucode(), envelope.payload().getProsecutionSubmissionDetails().getProsecutingAuthority())) {
            throw new ForbiddenRequestException("");
        }

        final SubmitChargeProsecutionHttpWithOucode payload = envelope.payload();

        final Map<String, List<String>> violations = new HashMap<>();

        final List<DefendantDetails> defendantDetails = payload
                .getDefendants()
                .stream()
                .map(ChargeDefendant::getDefendantDetails)
                .filter(Objects::nonNull)
                .map(Optional::ofNullable)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        defendantValidator.validate(defendantDetails, violations);

        payload.getDefendants().forEach(chargeDefendant ->
                offenceValidator
                        .validate(chargeDefendant.getOffences()
                                .stream()
                                .map(ChargeOffence::getOffenceDetails)
                                .collect(Collectors.toList()), violations));

        if (violations.size() > 0) {
            throwBadRequestException(violations);
        }

        final String urn = payload.getProsecutionSubmissionDetails().getUrn();
        final Pair<UUID, Boolean> submissionIdWithMatchFoundPair = systemIdMapperService.getSubmissionIdForUrnWithMatchFound(urn);
        final JsonObject submissionPayload = createObjectBuilder()
                .add(SUBMISSION_ID, submissionIdWithMatchFoundPair.getLeft().toString())
                .add(OUCODE, envelope.payload().getOucode())
                .build();
        final JsonObject submissionResponse = submissionQueryView.querySubmissionV2(submissionPayload);

        if (!submissionIdWithMatchFoundPair.getRight() || shouldRaiseSubmission(submissionResponse)) {
            final SubmitChargeProsecutionWithSubmissionId payloadWithSubmissionId =
                    submitChargeProsecutionWithSubmissionId()
                            .withDefendants(payload.getDefendants())
                            .withProsecutionSubmissionDetails(payload.getProsecutionSubmissionDetails())
                            .withSubmissionId(submissionIdWithMatchFoundPair.getLeft())
                            .build();

            sender.send(envelop(payloadWithSubmissionId)
                    .withName("stagingprosecutors.command.charge-prosecution")
                    .withMetadataFrom(envelope));

        }

        final UUID submissionId = submissionIdWithMatchFoundPair.getLeft();
        return Envelope.envelopeFrom(envelope.metadata(), new UrlResponse(getBaseResponseURLWithVersion() + submissionId.toString(), submissionId));
    }

    @Handles("hmcts.cjs.requisition-prosecution.v2")
    public Envelope<UrlResponse> submitRequisitionProsecution(final Envelope<SubmitRequisitionProsecutionHttpWithOucode> envelope) {
        if (!StringUtils.equalsIgnoreCase(envelope.payload().getOucode(), envelope.payload().getProsecutionSubmissionDetails().getProsecutingAuthority())) {
            throw new ForbiddenRequestException("");
        }

        final SubmitRequisitionProsecutionHttpWithOucode payload = envelope.payload();

        final Map<String, List<String>> violations = new HashMap<>();

        final List<DefendantDetails> defendantDetails = payload
                .getDefendants()
                .stream()
                .map(RequisitionDefendant::getDefendantDetails)
                .filter(Objects::nonNull)
                .map(Optional::ofNullable)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        defendantValidator.validate(defendantDetails, violations);

        payload.getDefendants().forEach(chargeDefendant ->
                offenceValidator
                        .validate(chargeDefendant.getOffences()
                                .stream()
                                .map(RequisitionOffence::getOffenceDetails)
                                .collect(Collectors.toList()), violations));

        if (violations.size() > 0) {
            throwBadRequestException(violations);
        }

        final String urn = payload.getProsecutionSubmissionDetails().getUrn();
        final Pair<UUID, Boolean> submissionIdWithMatchFoundPair = systemIdMapperService.getSubmissionIdForUrnWithMatchFound(urn);

        final JsonObject submissionPayload = createObjectBuilder()
                .add(SUBMISSION_ID, submissionIdWithMatchFoundPair.getLeft().toString())
                .add(OUCODE, envelope.payload().getOucode())
                .build();
        final JsonObject submissionResponse = submissionQueryView.querySubmissionV2(submissionPayload);

        if (!submissionIdWithMatchFoundPair.getRight() || shouldRaiseSubmission(submissionResponse)) {

            final SubmitRequisitionProsecutionWithSubmissionId payloadWithSubmissionId =
                    submitRequisitionProsecutionWithSubmissionId()
                            .withDefendants(payload.getDefendants())
                            .withProsecutionSubmissionDetails(payload.getProsecutionSubmissionDetails())
                            .withSubmissionId(submissionIdWithMatchFoundPair.getLeft())
                            .build();

            sender.send(envelop(payloadWithSubmissionId)
                    .withName("stagingprosecutors.command.requisition-prosecution")
                    .withMetadataFrom(envelope));
        }
        final UUID submissionId = submissionIdWithMatchFoundPair.getLeft();

        return Envelope.envelopeFrom(envelope.metadata(), new UrlResponse(getBaseResponseURLWithVersion() + submissionId.toString(), submissionId));
    }

    @Handles("hmcts.cjs.sjp-prosecution.v2")
    public Envelope<UrlResponse> submitSJPProsecution(final Envelope<SubmitSjpProsecutionHttpWithOucode> envelope) {

        if (!StringUtils.equalsIgnoreCase(envelope.payload().getOucode(), envelope.payload().getProsecutionSubmissionDetails().getProsecutingAuthority())) {
            throw new ForbiddenRequestException("");
        }

        final SubmitSjpProsecutionHttpV2 payload = submitSjpProsecutionHttpV2()
                .withProsecutionSubmissionDetails(envelope.payload().getProsecutionSubmissionDetails())
                .withDefendant(envelope.payload().getDefendant())
                .build();
        final Map<String, List<String>> violations = submitSjpProsecutionHttpV2Validator.validate(payload);

        if (violations.size() > 0) {
            throwBadRequestException(violations);
        }

        final String urn = payload.getProsecutionSubmissionDetails().getUrn();
        final Pair<UUID, Boolean> submissionIdWithMatchFoundPair = systemIdMapperService.getSubmissionIdForUrnWithMatchFound(urn);


        final JsonObject submissionPayload = createObjectBuilder()
                .add(SUBMISSION_ID, submissionIdWithMatchFoundPair.getLeft().toString())
                .add(OUCODE, envelope.payload().getOucode())
                .build();
        final JsonObject submissionResponse = submissionQueryView.querySubmissionV2(submissionPayload);

        if (!submissionIdWithMatchFoundPair.getRight() || shouldRaiseSubmission(submissionResponse)) {
            final Pair<SubmitSjpProsecutionHttpV2, UUID> payloadAnsSubmissionIdPair = new ImmutablePair<>(payload, submissionIdWithMatchFoundPair.getLeft());
            final SubmitSjpProsecution payloadWithSubmissionId = submitSjpProsecutionV2Converter.convert(payloadAnsSubmissionIdPair);

            sender.send(envelop(payloadWithSubmissionId)
                    .withName("stagingprosecutors.command.sjp-prosecution")
                    .withMetadataFrom(envelope));
        }
        final UUID submissionId = submissionIdWithMatchFoundPair.getLeft();
        return Envelope.envelopeFrom(envelope.metadata(), new UrlResponse(getBaseResponseURLWithVersion() + submissionId.toString(), submissionId));
    }

    @Handles("stagingprosecutors.submit-material-with-ptiurn")
    public Envelope<UrlResponse> submitMaterialWithPtiUrn(final JsonEnvelope envelope) {
        final String defendantIdField = "defendantId";

        final JsonObject requestPayload = envelope.payloadAsJsonObject();

        try {
            jsonSchemaValidator.validate(requestPayload.toString(), envelope.metadata().name());
        } catch (JsonSchemaValidationException e) {
            throw new BadRequestException("Error submitting material, request has schema violations", e);
        }

        final String ptiUrn = requestPayload.getString("ptiUrn");
        final UUID submissionId = uuidProducer.generateUUID();

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add(SUBMISSION_ID, submissionId.toString())
                .add("materialId", requestPayload.getString("material"))
                .add(CASE_URN, ptiUrn)
                .add(MATERIAL_TYPE, requestPayload.getString(MATERIAL_TYPE));

        if (requestPayload.containsKey(defendantIdField)) {
            payloadBuilder.add(defendantIdField, requestPayload.getString(defendantIdField));
        }

        if (requestPayload.containsKey(SRC_DOCUMENT_REFERENCE_FIELD)) {
            systemIdMapperService.attemptAddMappingForSrcDocumentReference(requestPayload.getString(SRC_DOCUMENT_REFERENCE_FIELD), submissionId);
        }

        sender.send(envelop(payloadBuilder.build())
                .withName("stagingprosecutors.command.submit-material")
                .withMetadataFrom(envelope));


        return envelopeFrom(
                envelope.metadata(),
                new UrlResponse(getBaseResponseURLWithVersion() + submissionId, submissionId));
    }


    @Handles("stagingprosecutors.submit-material.v2")
    public Envelope<UrlResponse> submitMaterial(final JsonEnvelope envelope) {
        final String defendantIdField = "defendantId";

        final JsonObject requestPayload = envelope.payloadAsJsonObject();

        if (!StringUtils.equalsIgnoreCase(requestPayload.getString(OUCODE), requestPayload.getString(PROSECUTING_AUTHORITY_FIELD))) {
            throw new ForbiddenRequestException("");
        }

        try {
            jsonSchemaValidator.validate(requestPayload.toString(), envelope.metadata().name());
        } catch (JsonSchemaValidationException e) {
            throw new BadRequestException("Error submitting material, request has schema violations", e);
        }

        final UUID submissionId = uuidProducer.generateUUID();

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add(SUBMISSION_ID, submissionId.toString())
                .add("materialId", requestPayload.getString("material"))
                .add(CASE_URN, requestPayload.getString(CASE_URN))
                .add(PROSECUTING_AUTHORITY_FIELD, requestPayload.getString(PROSECUTING_AUTHORITY_FIELD))
                .add(MATERIAL_TYPE, requestPayload.getString(MATERIAL_TYPE));

        if (requestPayload.containsKey(defendantIdField)) {
            payloadBuilder.add(defendantIdField, requestPayload.getString(defendantIdField));
        }

        if (requestPayload.containsKey(SRC_DOCUMENT_REFERENCE_FIELD)) {
            systemIdMapperService.attemptAddMappingForSrcDocumentReference(requestPayload.getString(SRC_DOCUMENT_REFERENCE_FIELD), submissionId);
        }

        sender.send(envelop(payloadBuilder.build())
                .withName("stagingprosecutors.command.submit-material")
                .withMetadataFrom(envelope));


        return envelopeFrom(
                envelope.metadata(),
                new UrlResponse(getBaseResponseURLWithVersion() + submissionId, submissionId));
    }

    private void throwBadRequestException(final Map<String, List<String>> violations) {
        try {
            throw new BadRequestException(mapper.writeValueAsString(violations));
        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to serialize violations json object", e);
            throw new BadRequestException("Business validations failed");
        }
    }

    private String getBaseResponseURLWithVersion() {
        return this.baseResponseURL.replace(RESPONSE_URL_VERSION_PLACEHOLDER, VERSION_NO);
    }

    private boolean shouldRaiseSubmission(final JsonObject submissionStatusResponse) {
        return  Objects.isNull(submissionStatusResponse) || Objects.nonNull(submissionStatusResponse.getString(STATUS)) && submissionStatusResponse.getString(STATUS).equals(SubmissionStatus.REJECTED.toString());
    }
}
