package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.FormType.BCM;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.ProgressionPublicEventProcessor.UPDATE_SUBMISSION_STATUS_COMMAND;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.ObjectBuilder.buildSubmitApplicationValidationFailed;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.ObjectBuilder.getMetadata;
import static uk.gov.moj.cps.progression.domain.event.CotrCreated.cotrCreated;
import static uk.gov.moj.cps.progression.domain.event.FormCreated.formCreated;
import static uk.gov.moj.cps.progression.domain.event.FormDefendants.formDefendants;

import uk.gov.justice.core.courts.CotrOperationFailed;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.ErrorDetails;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.util.ReferenceDataQueryService;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveMaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.UpdateSubmissionStatus;
import uk.gov.moj.cps.progression.domain.event.CotrCreated;
import uk.gov.moj.cps.progression.domain.event.CotrReviewNotesUpdated;
import uk.gov.moj.cps.progression.domain.event.CourtApplicationCreated;
import uk.gov.moj.cps.progression.domain.event.FormCreated;
import uk.gov.moj.cps.progression.domain.event.FormOperationFailed;
import uk.gov.moj.cps.progression.domain.event.PetFormCreated;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ApplicationSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationValidationFailed;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import dto.ResponseDto;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import service.CpsApiService;
import service.CpsPayloadTransformService;
import service.CpsPayloadTransformServiceTest;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"squid:S1607"})
public class ProgressionPublicEventProcessorTest {

    private static final UUID SUBMISSION_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final String APPLICATION_VALIDATION_FAIL_EVENT = "public.prosecutioncasefile.submit-application-validation-failed";

    @Mock
    private Sender sender;

    @Mock
    private CpsApiService cpsApiService;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Envelope<CourtApplicationCreated> courtApplicationCreatedEnvelope;

    @InjectMocks
    private ProgressionPublicEventProcessor progressionPublicEventProcessor;

    @Mock
    private CpsPayloadTransformService cpsPayloadTransformService = new CpsPayloadTransformService();

    @Captor
    private ArgumentCaptor<Envelope<ReceiveMaterialSubmissionSuccessful>> materialSubmissionCaptor;

    @Mock
    private ResponseDto response;

    @Mock
    private Requester requester;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;


    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Mock
    private JsonObject jsonObject;


    @Captor
    private ArgumentCaptor<Envelope<UpdateSubmissionStatus>> updateSubmissionCaptor;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @Test
    public void shouldHandlePublicSjpCaseCreatedEvent() {
        assertThat(progressionPublicEventProcessor, isHandler(EVENT_PROCESSOR)
                .with(method("caseDocumentUploaded")
                        .thatHandles("public.progression.court-document-added")
                ));
    }

    @Test
    public void shouldSendReceiveMaterialSubmissionSuccessfulCommand() {
        final Metadata metadataJsonObject = createMetaData("public.progression.court-document-added");

        final JsonEnvelope caseDocumentUploadedEnvelope = envelopeFrom(metadataJsonObject, JsonValue.NULL);

        progressionPublicEventProcessor.caseDocumentUploaded(caseDocumentUploadedEnvelope);

        verify(sender).send(materialSubmissionCaptor.capture());

        final Envelope<ReceiveMaterialSubmissionSuccessful> envelope = materialSubmissionCaptor.getValue();

        final Metadata metadata = envelope.metadata();
        final ReceiveMaterialSubmissionSuccessful receiveMaterialSubmissionSuccessful = envelope.payload();

        assertThat(metadata.streamId(), is(caseDocumentUploadedEnvelope.metadata().streamId()));
        assertThat(metadata.name(), is("stagingprosecutors.command.receive-material-submission-successful"));

        assertThat(receiveMaterialSubmissionSuccessful.getSubmissionId().toString(), is(SUBMISSION_ID.toString()));
    }

    @Test
    public void shouldNotifyAPIMWhenApplicationCreatedWithApplicantAsCPS() {
        final JsonObject payload = getPayload("json/progression-court-application-proceeding-initiated.json", randomUUID());
        final Metadata metadataJsonObject = metadataFrom(
                JsonObjects.createObjectBuilder(metadataWithRandomUUID("public.progression.court-application-created").build().asJsonObject())
                        .add("courtApplicationCreated", payload)
                        .build())
                .build();

        final JsonEnvelope courtApplicationCreatedEnvelope = envelopeFrom(metadataJsonObject, payload);

        final UUID applicantCps = randomUUID();

        when(referenceDataQueryService.getCPSProsecutors(courtApplicationCreatedEnvelope, requester)).thenReturn(Optional.of(createArrayBuilder()
                .add(createObjectBuilder().add("id", randomUUID().toString()).build())
                .add(createObjectBuilder().add("id", applicantCps.toString()).build())
                .add(createObjectBuilder().add("id", "9d38428b-7f9d-3aab-8269-f1427dd15c57").build())
                .build()));

        when(cpsApiService.sendApplicationCreatedNotification(any(), anyString())).thenReturn(response);
        when(featureControlGuard.isFeatureEnabled(any())).thenReturn(true);

        progressionPublicEventProcessor.notifyCourtApplicationCreated(courtApplicationCreatedEnvelope);

        verify(cpsApiService).sendApplicationCreatedNotification(any(), any());
    }

    @Test
    public void shouldNotifyAPIMWhenApplicationCreatedWithRespondentAsCPS() {
        final JsonObject payload = getPayload("json/progression-court-application-proceeding-initiated.json", randomUUID());
        final Metadata metadataJsonObject = metadataFrom(
                JsonObjects.createObjectBuilder(metadataWithRandomUUID("public.progression.court-application-created").build().asJsonObject())
                        .add("courtApplicationCreated", payload)
                        .add("submissionId", SUBMISSION_ID.toString())
                        .build())
                .build();

        final JsonEnvelope courtApplicationCreatedEnvelope = envelopeFrom(metadataJsonObject, payload);
        final UUID respondentProsecutorId = randomUUID();

        when(featureControlGuard.isFeatureEnabled(any())).thenReturn(true);
        when(referenceDataQueryService.getCPSProsecutors(courtApplicationCreatedEnvelope, requester)).thenReturn(Optional.of(createArrayBuilder()
                .add(createObjectBuilder().add("id", randomUUID().toString()).build())
                .add(createObjectBuilder().add("id", respondentProsecutorId.toString()).build())
                .add(createObjectBuilder().add("id", "9d38428b-7f9d-3aab-8269-f1427dd15c57").build())
                .build()));
        when(cpsApiService.sendApplicationCreatedNotification(any(), anyString())).thenReturn(response);

        progressionPublicEventProcessor.notifyCourtApplicationCreated(courtApplicationCreatedEnvelope);

        verify(cpsApiService).sendApplicationCreatedNotification(any(), any());
    }

    @Test
    public void shouldHandleProsecutionCaseFileValidationFailedEvent() {
        final JsonObject payload = getPayload("json/pcf-submit-application-validation-failed-not-related-to-defendant.json", randomUUID());
        final ApplicationSubmitted applicationSubmitted = jsonObjectToObjectConverter.convert(payload.getJsonObject("applicationSubmitted"), ApplicationSubmitted.class);
        final ErrorDetails errorDetails = jsonObjectToObjectConverter.convert(payload.getJsonObject("errorDetails"), ErrorDetails.class);

        final SubmitApplicationValidationFailed submitApplicationValidationFailed = buildSubmitApplicationValidationFailed(errorDetails, applicationSubmitted);
        final Envelope<SubmitApplicationValidationFailed> envelope
                = Envelope.envelopeFrom(getMetadata(APPLICATION_VALIDATION_FAIL_EVENT), submitApplicationValidationFailed);

        when(cpsApiService.sendApplicationCreatedNotification(any(), anyString())).thenReturn(response);
        progressionPublicEventProcessor.handleApplicationValidationFailed(envelope);

        verify(cpsApiService).sendApplicationCreatedNotification(any(), anyString());

        assertThat(progressionPublicEventProcessor, isHandler(EVENT_PROCESSOR)
                .with(method("handleApplicationValidationFailed")
                        .thatHandles(APPLICATION_VALIDATION_FAIL_EVENT)
                ));
    }

    @Test
    public void shouldHandleProsecutionCaseFileValidationFailedEventWhenTheErrorNotRelatedToDefendantAsnNotFoundAndDetailsNotFound() {
        final JsonObject payload = getPayload("json/pcf-submit-application-validation-failed.json", randomUUID());
        final ApplicationSubmitted applicationSubmitted = jsonObjectToObjectConverter.convert(payload.getJsonObject("applicationSubmitted"), ApplicationSubmitted.class);
        final ErrorDetails errorDetails = jsonObjectToObjectConverter.convert(payload.getJsonObject("errorDetails"), ErrorDetails.class);

        final SubmitApplicationValidationFailed submitApplicationValidationFailed = buildSubmitApplicationValidationFailed(errorDetails, applicationSubmitted);
        final Envelope<SubmitApplicationValidationFailed> envelope
                = Envelope.envelopeFrom(getMetadata(APPLICATION_VALIDATION_FAIL_EVENT), submitApplicationValidationFailed);
        when(cpsApiService.sendApplicationCreatedNotification(any(), anyString())).thenReturn(response);
        progressionPublicEventProcessor.handleApplicationValidationFailed(envelope);

        verify(cpsApiService).sendApplicationCreatedNotification(any(), anyString());

        assertThat(progressionPublicEventProcessor, isHandler(EVENT_PROCESSOR)
                .with(method("handleApplicationValidationFailed")
                        .thatHandles(APPLICATION_VALIDATION_FAIL_EVENT)
                ));
    }

    private static JsonObject getPayload(final String path, final UUID hearingId) {
        String request = null;
        try {
            final InputStream inputStream = CpsPayloadTransformServiceTest.class.getClassLoader().getResourceAsStream(path);
            assertThat(inputStream, IsNull.notNullValue());
            request = IOUtils.toString(inputStream, defaultCharset()).replace("HEARING_ID", hearingId.toString());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = createReader(new StringReader(request));
        return reader.readObject();
    }

    @Test
    public void shouldUpdateRejectSubmissionStatusCommand() {

        final FormOperationFailed publicFormOperationFailed = buildPublicFormOperationFailed().build();
        final Metadata metadataJsonObject = createMetaData("public.progression.form-operation-failed");

        final Envelope<FormOperationFailed> publicProsecutionRejectedEnvelope = Envelope.envelopeFrom(metadataJsonObject, publicFormOperationFailed);
        progressionPublicEventProcessor.formOperationFailed(publicProsecutionRejectedEnvelope);
        verify(sender).send(updateSubmissionCaptor.capture());

        final Envelope<UpdateSubmissionStatus> envelope = updateSubmissionCaptor.getValue();

        final Metadata metadata = envelope.metadata();
        final UpdateSubmissionStatus updateSubmissionStatusCommand = envelope.payload();

        assertThat(metadata.streamId(), is(envelope.metadata().streamId()));
        assertThat(metadata.name(), is(UPDATE_SUBMISSION_STATUS_COMMAND));

        assertThat(updateSubmissionStatusCommand.getSubmissionId().toString(), is(SUBMISSION_ID.toString()));
    }

    @Test
    public void shouldUpdateSuccessSubmissionStatusCommand() {

        final FormCreated formCreated = formCreated()
                .withSubmissionId(SUBMISSION_ID)
                .withCaseId(randomUUID())
                .withCourtFormId(randomUUID())
                .withFormData("{ \"firstNameTxt\": \"John\",  \"middleNameTxt\": \"Fed\", \"lastNameTxt\": \"Smith\"}")
                .withFormDefendants(asList(formDefendants()
                        .withDefendantId(randomUUID())
                        .withOffenceIds(asList(randomUUID()))
                        .build()))
                .withFormId(randomUUID())
                .withFormType(BCM)
                .withUserId(randomUUID())
                .build();
        final Metadata metadataJsonObject = createMetaData("public.progression.form-created");

        final Envelope<FormCreated> formCreatedEnvelope = Envelope.envelopeFrom(metadataJsonObject, formCreated);
        progressionPublicEventProcessor.formCreated(formCreatedEnvelope);
        verify(sender).send(updateSubmissionCaptor.capture());

        final Envelope<UpdateSubmissionStatus> envelope = updateSubmissionCaptor.getValue();

        final Metadata metadata = envelope.metadata();
        final UpdateSubmissionStatus updateSubmissionStatusCommand = envelope.payload();

        assertThat(metadata.streamId(), is(envelope.metadata().streamId()));
        assertThat(metadata.name(), is(UPDATE_SUBMISSION_STATUS_COMMAND));

        assertThat(updateSubmissionStatusCommand.getSubmissionId().toString(), is(SUBMISSION_ID.toString()));
    }

    @Test
    public void shouldUpdatePetSuccessSubmissionStatusCommand() {

        final PetFormCreated petFormCreated = buildPublicPetFormCreated().build();
        final Metadata metadataJsonObject = createMetaData("public.progression.pet-form-created");

        final Envelope<PetFormCreated> petFormCreatedEnvelope = Envelope.envelopeFrom(metadataJsonObject, petFormCreated);
        progressionPublicEventProcessor.petFormCreated(petFormCreatedEnvelope);
        verify(sender).send(updateSubmissionCaptor.capture());

        final Envelope<UpdateSubmissionStatus> envelope = updateSubmissionCaptor.getValue();

        final Metadata metadata = envelope.metadata();
        final UpdateSubmissionStatus updateSubmissionStatusCommand = envelope.payload();

        assertThat(metadata.streamId(), is(envelope.metadata().streamId()));
        assertThat(metadata.name(), is(ProgressionPublicEventProcessor.UPDATE_SUBMISSION_STATUS_COMMAND));

        assertThat(updateSubmissionStatusCommand.getSubmissionId().toString(), is(SUBMISSION_ID.toString()));

    }

    @Test
    public void shouldUpdateCotrReviewNotesSuccessSubmissionStatusCommand() {

        final CotrReviewNotesUpdated cotrReviewNotesUpdated = buildPublicCotrReviewNotesUpdate().build();
        final Metadata metadataJsonObject = createMetaData("public.progression.cotr-review-notes-updated");

        final Envelope<CotrReviewNotesUpdated> cotrReviewNotesUpdatedEnvelope = Envelope.envelopeFrom(metadataJsonObject, cotrReviewNotesUpdated);
        progressionPublicEventProcessor.cotrUpdateReviewStatusSuccessStatus(cotrReviewNotesUpdatedEnvelope);
        verify(sender).send(updateSubmissionCaptor.capture());

        final Envelope<UpdateSubmissionStatus> envelope = updateSubmissionCaptor.getValue();

        final Metadata metadata = envelope.metadata();
        final UpdateSubmissionStatus updateSubmissionStatusCommand = envelope.payload();

        assertEquals(envelope.metadata().streamId(), metadata.streamId());
        assertEquals(UPDATE_SUBMISSION_STATUS_COMMAND, metadata.name());
        assertEquals(SUBMISSION_ID, updateSubmissionStatusCommand.getSubmissionId());
    }

    @Test
    public void shouldUpdateCotrSuccessSubmissionStatusCommand() {

        final CotrCreated cotrCreated = cotrCreated()
                .withSubmissionId(SUBMISSION_ID)
                .withCotrId(randomUUID())
                .build();
        final Metadata metadataJsonObject = createMetaData("public.progression.cotr-created");

        final Envelope<CotrCreated> cotrCreatedEnvelope = Envelope.envelopeFrom(metadataJsonObject, cotrCreated);
        progressionPublicEventProcessor.cotrCreated(cotrCreatedEnvelope);
        verify(sender).send(updateSubmissionCaptor.capture());

        final Envelope<UpdateSubmissionStatus> envelope = updateSubmissionCaptor.getValue();

        final Metadata metadata = envelope.metadata();
        final UpdateSubmissionStatus updateSubmissionStatusCommand = envelope.payload();

        assertEquals(envelope.metadata().streamId(), metadata.streamId());
        assertEquals(UPDATE_SUBMISSION_STATUS_COMMAND, metadata.name());
        assertEquals(SUBMISSION_ID, updateSubmissionStatusCommand.getSubmissionId());
    }

    @Test
    public void shouldUpdateCotrRejectSubmissionStatusCommand() {

        final CotrOperationFailed publicFormOperationFailed = buildPublicCotrOperationFailed().build();
        final Metadata metadataJsonObject = createMetaData("public.progression.cotr-operation-failed");

        final Envelope<CotrOperationFailed> publicProsecutionRejectedEnvelope = Envelope.envelopeFrom(metadataJsonObject, publicFormOperationFailed);
        progressionPublicEventProcessor.cotrFormOperationFailed(publicProsecutionRejectedEnvelope);
        verify(sender).send(updateSubmissionCaptor.capture());

        final Envelope<UpdateSubmissionStatus> envelope = updateSubmissionCaptor.getValue();

        final Metadata metadata = envelope.metadata();
        final UpdateSubmissionStatus updateSubmissionStatusCommand = envelope.payload();

        assertThat(metadata.streamId(), is(envelope.metadata().streamId()));
        assertThat(metadata.name(), is(UPDATE_SUBMISSION_STATUS_COMMAND));

        assertThat(updateSubmissionStatusCommand.getSubmissionId().toString(), is(SUBMISSION_ID.toString()));
    }

    private Metadata createMetaData(String eventName) {
        return metadataFrom(
                JsonObjects.createObjectBuilder(metadataWithRandomUUID(eventName).build().asJsonObject())
                        .add("submissionId", SUBMISSION_ID.toString())
                        .build())
                .build();
    }

    private PetFormCreated.Builder buildPublicPetFormCreated() {
        return PetFormCreated.petFormCreated()
                .withSubmissionId(SUBMISSION_ID).withCaseId(randomUUID()).withPetId(randomUUID());
    }

    private FormOperationFailed.Builder buildPublicFormOperationFailed() {
        return FormOperationFailed.formOperationFailed()
                .withSubmissionId(SUBMISSION_ID).withFormType(BCM).withCaseId(randomUUID()).withCourtFormId(randomUUID());
    }

    private CotrOperationFailed.Builder buildPublicCotrOperationFailed() {
        return CotrOperationFailed.cotrOperationFailed()
                .withSubmissionId(SUBMISSION_ID).withCaseId(CASE_ID);

    }

    private CotrReviewNotesUpdated.Builder buildPublicCotrReviewNotesUpdate() {
        return CotrReviewNotesUpdated.cotrReviewNotesUpdated()
                .withSubmissionId(SUBMISSION_ID).withCotrId(randomUUID());

    }

    @Test
    public void shouldUpdateCotrRejectSubmissionStatusCommand_CotrIdNotFound() {

        final CotrOperationFailed publicFormOperationFailed = buildPublicCotrOperationFailed()
                .withMessage("COTR_ID_NOT_FOUND")
                .build();
        final Metadata metadataJsonObject = createMetaData("public.progression.cotr-operation-failed");

        final Envelope<CotrOperationFailed> publicProsecutionRejectedEnvelope = Envelope.envelopeFrom(metadataJsonObject, publicFormOperationFailed);
        progressionPublicEventProcessor.cotrFormOperationFailed(publicProsecutionRejectedEnvelope);
        verify(sender).send(updateSubmissionCaptor.capture());

        final Envelope<UpdateSubmissionStatus> envelope = updateSubmissionCaptor.getValue();

        final Metadata metadata = envelope.metadata();
        final UpdateSubmissionStatus updateSubmissionStatusCommand = envelope.payload();

        assertThat(updateSubmissionStatusCommand.getSubmissionId().toString(), is(SUBMISSION_ID.toString()));
        assertThat(updateSubmissionStatusCommand.getErrors().get(0).getCode(), is("FORM_DOES_NOT_EXIST"));
    }

    @Test
    public void shouldUpdateCotrRejectSubmissionStatusCommand_HearingNotFound() {

        final CotrOperationFailed publicFormOperationFailed = buildPublicCotrOperationFailed()
                .withMessage("HEARING_ID_NOT_FOUND")
                .build();
        final Metadata metadataJsonObject = createMetaData("public.progression.cotr-operation-failed");

        final Envelope<CotrOperationFailed> publicProsecutionRejectedEnvelope = Envelope.envelopeFrom(metadataJsonObject, publicFormOperationFailed);
        progressionPublicEventProcessor.cotrFormOperationFailed(publicProsecutionRejectedEnvelope);
        verify(sender).send(updateSubmissionCaptor.capture());

        final Envelope<UpdateSubmissionStatus> envelope = updateSubmissionCaptor.getValue();

        final Metadata metadata = envelope.metadata();
        final UpdateSubmissionStatus updateSubmissionStatusCommand = envelope.payload();

        assertThat(updateSubmissionStatusCommand.getSubmissionId().toString(), is(SUBMISSION_ID.toString()));
        assertThat(updateSubmissionStatusCommand.getErrors().get(0).getCode(), is("PROVIDE_HEARING_DETAILS"));
    }
}