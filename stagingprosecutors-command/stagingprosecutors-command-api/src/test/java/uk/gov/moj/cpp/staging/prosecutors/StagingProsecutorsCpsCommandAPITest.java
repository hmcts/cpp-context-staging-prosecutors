package uk.gov.moj.cpp.staging.prosecutors;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.uuid.UUIDProducer;

import java.util.UUID;

import javax.json.JsonObject;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsCpsCommandAPITest {

    private static final UUID SUBMISSION_ID = randomUUID();
    private static final UUID MATERIAL_ID = randomUUID();
    private static final String OUCODE = "B01BH00";

    @Mock
    private Sender sender;

    @Mock
    private UUIDProducer uuidProducer;

    @Mock
    private JsonSchemaValidator jsonSchemaValidator;

    @InjectMocks
    private StagingProsecutorsCpsCommandApi stagingProsecutorsCpsCommandApi;

    @Captor
    private ArgumentCaptor<Envelope> materialEnvelopeCaptor;

    @Test
    public void shouldHandleSubmitMaterial() {

        stagingProsecutorsCpsCommandApi.baseResponseURL = "test-base-url/";

        when(uuidProducer.generateUUID()).thenReturn(SUBMISSION_ID);

        final JsonObject payload = createObjectBuilder()
                .add("material", MATERIAL_ID.toString())
                .add("caseUrn", "caseUrn01")
                .add("prosecutingAuthority", OUCODE)
                .add("materialType", "SJPN")
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material-cps.v1", payload);
        final Envelope<UrlResponse> submitMaterialResponseEnvelope = stagingProsecutorsCpsCommandApi.submitMaterial(requestEnvelope);

        //then
        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertThat(submitMaterialResponseEnvelope.payload().getStatusURL(), equalTo(expectedStatusURL));
        assertThat(submitMaterialResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));

        verify(sender).send(materialEnvelopeCaptor.capture());

        final Envelope sentEnvelope = materialEnvelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata(), withMetadataEnvelopedFrom(requestEnvelope)
                .withName("stagingprosecutors.command.submit-material"));

        final JsonObject payloadWithSubmissionId = createObjectBuilder()
                .add("submissionId", SUBMISSION_ID.toString())
                .add("materialId", MATERIAL_ID.toString())
                .add("caseUrn", "caseUrn01")
                .add("prosecutingAuthority", OUCODE)
                .add("materialType", "SJPN")
                .add("isCpsCase", Boolean.TRUE)
                .build();

        assertThat(sentEnvelope.payload(), equalTo(payloadWithSubmissionId));
    }

    @Test
    public void shouldThrowBadRequestExceptionIfRequestPayloadFailsSchemaValidation() {
        stagingProsecutorsCpsCommandApi.baseResponseURL = "test-base-url/";

        doThrow(new JsonSchemaValidationException("Schema violations"))
                .when(jsonSchemaValidator).validate(any(), eq("stagingprosecutors.submit-material-cps.v1"));

        final JsonObject invalidPayload = createObjectBuilder()
                .add("materialId", "not_A_valid_UUID")
                .add("prosecutingAuthority", OUCODE)
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material-cps.v1", invalidPayload);

        assertThrows(BadRequestException.class, () -> stagingProsecutorsCpsCommandApi.submitMaterial(requestEnvelope));
    }

}
