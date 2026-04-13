package uk.gov.moj.cpp.staging.prosecutors;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails.sjpProsecutionSubmissionDetails;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutorapi.query.view.SubmissionQueryView;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SjpProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutors.converter.SubmitSjpProsecutionConverter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttp;
import uk.gov.moj.cpp.staging.prosecutors.pojo.SubmitSjpProsecution;
import uk.gov.moj.cpp.staging.prosecutors.service.SystemIdMapperService;
import uk.gov.moj.cpp.staging.prosecutors.uuid.UUIDProducer;
import uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionHttpValidator;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmitApplication;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsCommandApiTest {

    private static final UUID SUBMISSION_ID = randomUUID();
    private static final UUID MATERIAL_ID = randomUUID();

    @Mock
    private Sender sender;

    @Mock
    private UUIDProducer uuidProducer;

    @Mock
    private SubmitSjpProsecutionConverter submitSjpProsecutionConverter;

    @Mock
    private SubmitSjpProsecutionHttpValidator submitSjpProsecutionHttpValidator;

    @Mock
    private JsonSchemaValidator jsonSchemaValidator;

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @Mock
    private SubmissionQueryView submissionQueryView;

    @InjectMocks
    private StagingProsecutorsCommandApi stagingProsecutorsCommandApi;
    @Captor
    private ArgumentCaptor<Envelope<SubmitSjpProsecution>> sjpEnvelopeCaptor;
    @Captor
    private ArgumentCaptor<Envelope<SubmitApplication>> submitApplicationEnvelopeCaptor;
    @Captor
    private ArgumentCaptor<Envelope> materialEnvelopeCaptor;

    @Test
    public void isHandler() {
        assertThat(StagingProsecutorsCommandApi.class, isHandlerClass(COMMAND_API)
                .with(
                        method("submitProsecution")
                                .thatHandles("stagingprosecutors.submit-prosecution")
                )
                .with(
                        method("submitSJPProsecution")
                                .thatHandles("stagingprosecutors.submit-sjp-prosecution")
                ).with(
                        method("submitMaterial")
                                .thatHandles("stagingprosecutors.submit-material")
                )
        );
    }

    @Test
    public void handleSjpProsecution() {
        stagingProsecutorsCommandApi.baseResponseURL = "test-base-url/";
        final SubmitSjpProsecutionHttp payload = SubmitSjpProsecutionHttp
                .submitSjpProsecutionHttp()
                .withDefendant(SjpDefendant.sjpDefendant().build())
                .withProsecutionSubmissionDetails(SjpProsecutionSubmissionDetails.sjpProsecutionSubmissionDetails().build())
                .build();
        final SubmitSjpProsecution convertedPayload = SubmitSjpProsecution
                .submitSjpProsecution()
                .withDefendant(SjpDefendant.sjpDefendant().build())
                .withProsecutionSubmissionDetails(sjpProsecutionSubmissionDetails().build())
                .withSubmissionId(SUBMISSION_ID)
                .build();
        final Envelope<SubmitSjpProsecutionHttp> originalEnvelope = createSubmitSjpProsecutionEnvelope(payload);
        final Map<String, List<String>> noViolations = new HashMap<>();

        when(systemIdMapperService.getSubmissionIdForUrnWithMatchFound(payload.getProsecutionSubmissionDetails().getUrn())).thenReturn(new ImmutablePair<>(SUBMISSION_ID, Boolean.FALSE));

        final Pair<SubmitSjpProsecutionHttp, UUID> source = new ImmutablePair<>(payload, SUBMISSION_ID);
        final JsonObject submissionResponseObject = createObjectBuilder().add("status", SubmissionStatus.REJECTED.toString()).build();
        when(submissionQueryView.querySubmissionV2(any())).thenReturn(submissionResponseObject);
        when(submitSjpProsecutionConverter.convert(source)).thenReturn(convertedPayload);
        when(submitSjpProsecutionHttpValidator.validate(payload)).thenReturn(noViolations);

        final Envelope<UrlResponse> stagingProsecutorsResponseEnvelope = stagingProsecutorsCommandApi.submitSJPProsecution(originalEnvelope);

        //then
        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertThat(stagingProsecutorsResponseEnvelope.payload().getStatusURL(), equalTo(expectedStatusURL));
        assertThat(stagingProsecutorsResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));

        verify(sender).send(sjpEnvelopeCaptor.capture());

        final Envelope<SubmitSjpProsecution> sentEnvelope = sjpEnvelopeCaptor.getValue();
        final Metadata sentEnvelopeMetadata = sentEnvelope.metadata();
        final SubmitSjpProsecution sentEnvelopePayload = sentEnvelope.payload();

        assertThat(sentEnvelopeMetadata.name(), equalTo("stagingprosecutors.command.sjp-prosecution"));
        assertThat(sentEnvelopePayload, equalTo(convertedPayload));
    }

    @Test
    public void handleSjpProsecutionShouldThrowBadRequestErrorWhenThereAreViolations() {
        stagingProsecutorsCommandApi.baseResponseURL = "test-base-url/";
        Map<String, List<String>> violations = new LinkedHashMap<>();
        violations.put("violation name", singletonList("reason 1"));
        when(submitSjpProsecutionHttpValidator.validate(any())).thenReturn(violations);

        assertThrows(BadRequestException.class, () -> stagingProsecutorsCommandApi.submitSJPProsecution(mock(Envelope.class)));
    }

    @Test
    public void shouldHandleSubmitMaterial() {

        stagingProsecutorsCommandApi.baseResponseURL = "test-base-url/";
        when(uuidProducer.generateUUID()).thenReturn(SUBMISSION_ID);
        final JsonObject payload = createObjectBuilder()
                .add("material", MATERIAL_ID.toString())
                .add("caseUrn", "caseUrn01")
                .add("prosecutingAuthority", "prosecutingAuthority")
                .add("materialType", "SJPN")
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material", payload);
        final Envelope<UrlResponse> submitMaterialResponseEnvelope = stagingProsecutorsCommandApi.submitMaterial(requestEnvelope);

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
                .add("prosecutingAuthority", "prosecutingAuthority")
                .add("materialType", "SJPN")
                .build();

        assertThat(sentEnvelope.payload(), equalTo(payloadWithSubmissionId));
    }

    @Test
    public void shouldHandleSubmitApplication() {
        stagingProsecutorsCommandApi.baseResponseURL = "test-base-url/";
        final SubmitApplication incomingPayload = SubmitApplication
                .submitApplication()
                .withCourtApplication(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .build())
                .build();

        stagingProsecutorsCommandApi.submitApplication(createSubmitApplicationEnvelope(incomingPayload));

        verify(sender).send(submitApplicationEnvelopeCaptor.capture());

        final Envelope<SubmitApplication> sentEnvelope = submitApplicationEnvelopeCaptor.getValue();
        final Metadata sentEnvelopeMetadata = sentEnvelope.metadata();
        final SubmitApplication submitApplication = sentEnvelope.payload();

        assertThat(sentEnvelopeMetadata.name(), equalTo("stagingprosecutors.command.submit-application"));
        assertThat(submitApplication, is(incomingPayload));
    }

    @Test
    public void shouldHandleSubmitMaterialWithOptionalMetadataFields() {

        stagingProsecutorsCommandApi.baseResponseURL = "test-base-url/";
        when(uuidProducer.generateUUID()).thenReturn(SUBMISSION_ID);
        final JsonObject payload = createObjectBuilder()
                .add("material", MATERIAL_ID.toString())
                .add("caseUrn", "caseUrn01")
                .add("prosecutingAuthority", "prosecutingAuthority")
                .add("materialType", "SJPN")
                .add("defendantId", "DefendantA001")
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material", payload);
        final Envelope<UrlResponse> submitMaterialResponseEnvelope = stagingProsecutorsCommandApi.submitMaterial(requestEnvelope);

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
                .add("prosecutingAuthority", "prosecutingAuthority")
                .add("materialType", "SJPN")
                .add("defendantId", "DefendantA001")
                .build();

        assertThat(sentEnvelope.payload(), equalTo(payloadWithSubmissionId));
    }

    @Test
    public void shouldThrowBadRequestExceptionIfRequestPayloadFailsSchemaValidation() {
        stagingProsecutorsCommandApi.baseResponseURL = "test-base-url/";
        doThrow(new JsonSchemaValidationException("Schema violations"))
                .when(jsonSchemaValidator).validate(any(), eq("stagingprosecutors.submit-material"));

        final JsonObject invalidPayload = createObjectBuilder()
                .add("materialId", "not_A_valid_UUID")
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material", invalidPayload);

        assertThrows(BadRequestException.class, () -> stagingProsecutorsCommandApi.submitMaterial(requestEnvelope));
    }

    private Envelope<SubmitSjpProsecutionHttp> createSubmitSjpProsecutionEnvelope(final SubmitSjpProsecutionHttp payload) {
        final Metadata metadata = Envelope.metadataBuilder().withId(randomUUID())
                .withName("stagingprosecutors.submit-sjp-prosecution")
                .createdAt(now()).build();

        return Envelope.envelopeFrom(metadata, payload);
    }

    private Envelope<SubmitApplication> createSubmitApplicationEnvelope(final SubmitApplication payload) {
        final Metadata metadata = Envelope.metadataBuilder().withId(randomUUID())
                .withName("stagingprosecutors.submit-application")
                .createdAt(now()).build();

        return Envelope.envelopeFrom(metadata, payload);
    }
}
