package uk.gov.moj.cpp.staging.prosecutors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmitApplication;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutorapi.query.view.SubmissionQueryView;
import uk.gov.moj.cpp.staging.prosecutors.converter.SubmitSjpProsecutionConverter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttp;
import uk.gov.moj.cpp.staging.prosecutors.pojo.SubmitSjpProsecution;
import uk.gov.moj.cpp.staging.prosecutors.service.SystemIdMapperService;
import uk.gov.moj.cpp.staging.prosecutors.uuid.UUIDProducer;
import uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionHttpValidator;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

@ServiceComponent(COMMAND_API)
public class StagingProsecutorsCommandApi {

    @Inject
    @Value(key = "stagingprosecutors.submit-prosecution-response.base-url", defaultValue = "https://replace-me.gov.uk/")
    String baseResponseURL;

    private static final String RESPONSE_URL_VERSION_PLACEHOLDER = "VERSION";
    private static final String VERSION_NO = "v1";

    @Inject
    private Sender sender;

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Inject
    private SubmitSjpProsecutionConverter submitSjpProsecutionConverter;

    @Inject
    private SubmitSjpProsecutionHttpValidator submitSjpProsecutionHttpValidator;

    @Inject
    private UUIDProducer uuidProducer;

    @Inject
    private JsonSchemaValidator jsonSchemaValidator;

    @Inject
    private SubmissionQueryView submissionQueryView;

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingProsecutorsCommandApi.class);

    private final ObjectMapper mapper = new ObjectMapperProducer().objectMapper().copy();

    private static final String STATUS = "status";

    @Handles("hmcts.cjs.sjp-prosecution")
    public Envelope<UrlResponse> submitSJPProsecution(final Envelope<SubmitSjpProsecutionHttp> envelope) {

        final SubmitSjpProsecutionHttp payload = envelope.payload();
        final Map<String, List<String>> violations = submitSjpProsecutionHttpValidator.validate(payload);

        if (violations.size() > 0) {
            throwBadRequestException(violations);
        }
        final String urn = payload.getProsecutionSubmissionDetails().getUrn();
        final Pair<UUID, Boolean> submissionIdWithMatchFoundPair = systemIdMapperService.getSubmissionIdForUrnWithMatchFound(urn);
        final JsonObject submissionPayload = createObjectBuilder()
                .add("submissionId", submissionIdWithMatchFoundPair.getLeft().toString())
                .build();
        final JsonObject submissionResponse = submissionQueryView.querySubmissionV2(submissionPayload);

        if (!submissionIdWithMatchFoundPair.getRight() || shouldRaiseSubmission(submissionResponse)) {
            final Pair<SubmitSjpProsecutionHttp, UUID> payloadAnsSubmissionIdPair = new ImmutablePair<>(payload, submissionIdWithMatchFoundPair.getLeft());
            final SubmitSjpProsecution payloadWithSubmissionId = submitSjpProsecutionConverter.convert(payloadAnsSubmissionIdPair);

            sender.send(envelop(payloadWithSubmissionId)
                    .withName("stagingprosecutors.command.sjp-prosecution")
                    .withMetadataFrom(envelope));
        }

        return Envelope.envelopeFrom(envelope.metadata(), new UrlResponse(getBaseResponseURLWithVersion() + submissionIdWithMatchFoundPair.getLeft().toString(), submissionIdWithMatchFoundPair.getLeft()));
    }


    @Handles("stagingprosecutors.submit-application")
    public void submitApplication(final Envelope<SubmitApplication> envelope) {
        sender.send(Enveloper.envelop(envelope.payload())
                .withName("stagingprosecutors.command.submit-application")
                .withMetadataFrom(envelope));
    }

    @Handles("stagingprosecutors.submit-material")
    public Envelope<UrlResponse> submitMaterial(final JsonEnvelope envelope) {
        final String defendantIdField = "defendantId";

        final JsonObject requestPayload = envelope.payloadAsJsonObject();

        try {
            jsonSchemaValidator.validate(requestPayload.toString(), envelope.metadata().name());
        } catch (JsonSchemaValidationException e) {
            throw new BadRequestException("Error submitting material, request has schema violations", e);
        }

        final UUID submissionId = uuidProducer.generateUUID();

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("materialId", requestPayload.getString("material"))
                .add("caseUrn", requestPayload.getString("caseUrn"))
                .add("prosecutingAuthority", requestPayload.getString("prosecutingAuthority"))
                .add("materialType", requestPayload.getString("materialType"));

        if (requestPayload.containsKey(defendantIdField)) {
            payloadBuilder.add(defendantIdField, requestPayload.getString(defendantIdField));
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
