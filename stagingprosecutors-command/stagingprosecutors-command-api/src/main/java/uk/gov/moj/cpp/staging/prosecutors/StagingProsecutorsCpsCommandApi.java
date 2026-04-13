package uk.gov.moj.cpp.staging.prosecutors;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.uuid.UUIDProducer;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;

@ServiceComponent(COMMAND_API)
public class StagingProsecutorsCpsCommandApi {

    @Inject
    @Value(key = "stagingprosecutors.submit-prosecution-response.base-url", defaultValue = "https://replace-me.gov.uk/")
    String baseResponseURL;

    private static final String RESPONSE_URL_VERSION_PLACEHOLDER = "VERSION";
    private static final String VERSION_NO = "v1";

    @Inject
    private Sender sender;

    @Inject
    private UUIDProducer uuidProducer;

    @Inject
    private JsonSchemaValidator jsonSchemaValidator;

    @Handles("stagingprosecutors.submit-material-cps.v1")
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
                .add("materialType", requestPayload.getString("materialType"))
                .add("isCpsCase", Boolean.TRUE);

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

    private String getBaseResponseURLWithVersion() {
        return this.baseResponseURL.replace(RESPONSE_URL_VERSION_PLACEHOLDER, VERSION_NO);
    }
}
