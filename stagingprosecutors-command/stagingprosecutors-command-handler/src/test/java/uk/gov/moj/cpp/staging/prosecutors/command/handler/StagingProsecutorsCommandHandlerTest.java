package uk.gov.moj.cpp.staging.prosecutors.command.handler;

import static cpp.moj.gov.uk.staging.prosecutors.json.schemas.ReceivePocaEmail.receivePocaEmail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSubmissionSuccessful.receiveSubmissionSuccessful;
import static uk.gov.moj.cpp.staging.prosecutors.test.utils.HandlerTestHelper.matchEvent;
import static uk.gov.moj.cpp.staging.prosecutors.test.utils.HandlerTestHelper.metadataFor;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplicationType;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitCpsServeBcm;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.service.ReferenceDataServiceImpl;
import uk.gov.moj.cpp.staging.prosecutors.domain.CpsSubmission;
import uk.gov.moj.cpp.staging.prosecutors.domain.PocaEmailAggregate;
import uk.gov.moj.cpp.staging.prosecutors.domain.ProsecutionSubmission;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeBcmReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePtphReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsUpdateCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.PocaDocumentValidated;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProblemValue;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSubmissionSuccessfulWithWarnings;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.RejectSubmission;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatusUpdated;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionSuccessfulWithWarnings;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecution;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.UpdateSubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.test.utils.FileResourceObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ReceivePocaEmail;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.command.SubmitApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsCommandHandlerTest {

    private static final UUID SUBMISSION_ID = UUID.fromString("8191c165-e9e4-4fd7-ac6d-5bfd04690f77");
    private static final UUID PROSECUTOR_DEFENDANT_ID = UUID.fromString("e191c165-e9e4-4fd7-ac6d-5bfd04690f78");
    private static final UUID POCA_MAIL_ID = UUID.fromString("8191c165-e9e4-4fd7-ac6d-5bfd04690f78");

    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            ProsecutionReceived.class,
            SjpProsecutionReceived.class,
            SubmissionSuccessful.class,
            SubmissionSuccessfulWithWarnings.class,
            SubmissionRejected.class,
            SubmissionStatusUpdated.class,
            CpsServePetReceived.class,
            CpsServeBcmReceived.class,
            CpsServePtphReceived.class,
            CpsServeCotrReceived.class,
            CpsUpdateCotrReceived.class,
            PocaDocumentValidated.class);
    @InjectMocks
    private
    StagingProsecutorsCommandHandler stagingProsecutorsCommandHandler;
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    @Mock
    private FileRetriever fileRetriever;

    @Mock
    private ReferenceDataServiceImpl referenceDataServiceImpl;

    @Test
    public void shouldHandlerReceiveAllegationsCommand() {
        assertThat(new StagingProsecutorsCommandHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleSjpProsecutionSubmission")
                        .thatHandles("stagingprosecutors.command.sjp-prosecution")
                ));
    }

    @Test
    public void shouldHandleReceiveSubmissionSuccessfulCommand() {
        assertThat(new StagingProsecutorsCommandHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleReceiveSubmissionSuccessful")
                        .thatHandles("stagingprosecutors.command.receive-submission-successful")
                ));
    }

    @Test
    public void shouldHandleChargeCaseSubmission() throws IOException, EventStreamException {

        final ProsecutionSubmission aggregate = new ProsecutionSubmission();
        when(eventSource.getStreamById(SUBMISSION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionSubmission.class)).thenReturn(aggregate);

        final SubmitChargeProsecution submitChargeProsecution =
                handlerTestHelper.convertFromFile("json/submitChargeProsecution.json", SubmitChargeProsecution.class);

        final Envelope<SubmitChargeProsecution> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.charge-prosecution", SUBMISSION_ID), submitChargeProsecution);

        stagingProsecutorsCommandHandler.handleChargeProsecutionSubmission(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.prosecution-received",
                handlerTestHelper.convertFromFile("json/charge_prosecution_received.json", JsonValue.class));
    }

    @Test
    public void shouldHandleSjpCaseSubmission() throws IOException, EventStreamException {

        final ProsecutionSubmission aggregate = new ProsecutionSubmission();
        when(eventSource.getStreamById(SUBMISSION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionSubmission.class)).thenReturn(aggregate);

        final SubmitSjpProsecution submitSjpProsecution =
                handlerTestHelper.convertFromFile("json/submitSjpProsecution.json", SubmitSjpProsecution.class);

        final Envelope<SubmitSjpProsecution> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.submit-sjp-prosecution", SUBMISSION_ID), submitSjpProsecution);
        stagingProsecutorsCommandHandler.handleSjpProsecutionSubmission(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.sjp-prosecution-received",
                handlerTestHelper.convertFromFile("json/sjpn_prosecution_received.json", JsonValue.class));
    }

    @Test
    public void shouldHandleReceiveSubmissionSuccessful() throws IOException, EventStreamException {
        final ProsecutionSubmission aggregate = new ProsecutionSubmission();
        when(eventSource.getStreamById(SUBMISSION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionSubmission.class)).thenReturn(aggregate);

        final SubmitSjpProsecution submitSjpProsecution =
                handlerTestHelper.convertFromFile("json/submitSjpProsecution.json", SubmitSjpProsecution.class);

        aggregate.receiveSjpSubmission(submitSjpProsecution.getSubmissionId(), null, null);

        final ReceiveSubmissionSuccessful receiveSubmissionSuccessful = receiveSubmissionSuccessful().withSubmissionId(SUBMISSION_ID)
                .build();

        final Envelope<ReceiveSubmissionSuccessful> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.receive-submission-successful", SUBMISSION_ID), receiveSubmissionSuccessful);

        stagingProsecutorsCommandHandler.handleReceiveSubmissionSuccessful(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.submission-successful",
                handlerTestHelper.convertFromFile("json/submission_successful_event.json", JsonValue.class));
    }

    @Test
    public void shouldHandleReceiveSubmissionSuccessfulWithWarnings() throws IOException, EventStreamException {

        final ProsecutionSubmission aggregate = new ProsecutionSubmission();
        final CpsSubmission cpsSubmission = new CpsSubmission();
        final PocaEmailAggregate pocaEmailAggregate = new PocaEmailAggregate();
        when(eventSource.getStreamById(SUBMISSION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionSubmission.class)).thenReturn(aggregate);

        final SubmitSjpProsecution submitSjpProsecution =
                handlerTestHelper.convertFromFile("json/submitSjpProsecution.json", SubmitSjpProsecution.class);

        aggregate.receiveSjpSubmission(submitSjpProsecution.getSubmissionId(), null, null);
        final Problem.Builder problem = Problem.problem();
        problem.withCode("DEFENDANT_BELOW_18");
        problem.withValues(ImmutableList.of(
                ProblemValue
                        .problemValue()
                        .withKey("dateOfBirth")
                        .withValue("2002-01-01")
                        .build()));
        final Problem warning = problem
                .build();

        final ReceiveSubmissionSuccessfulWithWarnings receiveSubmissionSuccessfulWithWarnings =
                ReceiveSubmissionSuccessfulWithWarnings
                        .receiveSubmissionSuccessfulWithWarnings()
                        .withSubmissionId(SUBMISSION_ID)
                        .withWarnings(ImmutableList.of(warning))
                        .build();

        final Metadata metadata = metadataFor("stagingprosecutors.command.receive-submission-successful-with-warnings", SUBMISSION_ID);

        final Envelope<ReceiveSubmissionSuccessfulWithWarnings> envelope = envelopeFrom(metadata, receiveSubmissionSuccessfulWithWarnings);

        stagingProsecutorsCommandHandler.handleReceiveSubmissionSuccessfulWithWarnings(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.submission-successful-with-warnings",
                handlerTestHelper.convertFromFile("json/submission_successful_event_with_warnings.json", JsonValue.class));
    }

    @Test
    public void shouldHandleRejectSubmission() throws IOException, EventStreamException {

        final ProsecutionSubmission aggregate = new ProsecutionSubmission();
        when(eventSource.getStreamById(SUBMISSION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionSubmission.class)).thenReturn(aggregate);

        final SubmitSjpProsecution submitSjpProsecution =
                handlerTestHelper.convertFromFile("json/submitSjpProsecution.json", SubmitSjpProsecution.class);

        aggregate.receiveSjpSubmission(submitSjpProsecution.getSubmissionId(), null, null);

        final Problem problem = Problem.problem()
                .withCode("DEFENDANT_DOB_IN_FUTURE")
                .withValues(ImmutableList.of(
                        ProblemValue
                                .problemValue()
                                .withKey("dateOfBirth")
                                .withValue("2050-01-01")
                                .build()))
                .build();

        final RejectSubmission rejectSubmission = RejectSubmission
                .rejectSubmission()
                .withSubmissionId(SUBMISSION_ID)
                .withErrors(ImmutableList.of(problem))
                .build();

        final Metadata metadata = metadataFor("stagingprosecutors.command.reject-submission", SUBMISSION_ID);
        final Envelope<RejectSubmission> envelope = envelopeFrom(metadata, rejectSubmission);

        stagingProsecutorsCommandHandler.handleRejectSubmission(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.submission-rejected",
                handlerTestHelper.convertFromFile("json/submission_rejected_event.json", JsonValue.class));
    }

    @Test
    public void shouldHandleRejectSubmissionForNonSjp() throws IOException, EventStreamException {

        final ProsecutionSubmission aggregate = new ProsecutionSubmission();
        when(eventSource.getStreamById(SUBMISSION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionSubmission.class)).thenReturn(aggregate);

        final SubmitChargeProsecution submitChargeProsecution =
                handlerTestHelper.convertFromFile("json/submitChargeProsecution.json", SubmitChargeProsecution.class);

        final Envelope<SubmitChargeProsecution> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.charge-prosecution", SUBMISSION_ID), submitChargeProsecution);

        aggregate.receiveSubmission(submitChargeProsecution.getSubmissionId(), null, null);


        final Problem defendantProblem = Problem.problem()
                .withCode("DEFENDANT_DOB_IN_FUTURE")
                .withValues(ImmutableList.of(
                        ProblemValue
                                .problemValue()
                                .withKey("dateOfBirth")
                                .withValue("2050-01-01")
                                .build()))
                .build();

        final Problem caseProblem = Problem.problem()
                .withCode("PROSECUTOR_OUCODE_NOT_RECOGNISED")
                .withValues(ImmutableList.of(
                        ProblemValue
                                .problemValue()
                                .withKey("originatingOrganisation")
                                .withValue("123")
                                .build()))
                .build();


        final DefendantProblem defendantError = DefendantProblem.defendantProblem()
                .withProblems(ImmutableList.of(defendantProblem))
                .withProsecutorDefendantReference(PROSECUTOR_DEFENDANT_ID.toString())
                .build();

        final RejectSubmission rejectSubmission = RejectSubmission
                .rejectSubmission()
                .withSubmissionId(SUBMISSION_ID)
                .withCaseErrors(ImmutableList.of(caseProblem))
                .withDefendantErrors(ImmutableList.of(defendantError))
                .build();

        final Metadata metadata = metadataFor("stagingprosecutors.command.reject-submission", SUBMISSION_ID);
        final Envelope<RejectSubmission> envelope2 = envelopeFrom(metadata, rejectSubmission);

        stagingProsecutorsCommandHandler.handleRejectSubmission(envelope2);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.submission-rejected",
                handlerTestHelper.convertFromFile("json/submission_rejected_event_nonsjp.json", JsonValue.class));
    }

    @Test
    public void shouldHandleCpsServePet() throws IOException, EventStreamException {
        final CpsSubmission cpsSubmission = new CpsSubmission();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CpsSubmission.class)).thenReturn(cpsSubmission);

        final SubmitCpsServePet submitCpsServePet =
                handlerTestHelper.convertFromFile("json/cps-serve-pet.json", SubmitCpsServePet.class);

        final Envelope<SubmitCpsServePet> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.submit-cps-serve-pet", SUBMISSION_ID), submitCpsServePet);

        stagingProsecutorsCommandHandler.handleServePet(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.cps-serve-pet-received",
                handlerTestHelper.convertFromFile("json/cps-serve-pet-received.json", JsonObject.class));
    }

    @Test
    public void shouldHandleCpsServeBcm() throws IOException, EventStreamException {
        final CpsSubmission cpsSubmission = new CpsSubmission();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CpsSubmission.class)).thenReturn(cpsSubmission);

        final SubmitCpsServeBcm submitCpsServeBcm =
                handlerTestHelper.convertFromFile("json/cps-serve-bcm.json", SubmitCpsServeBcm.class);

        final Envelope<SubmitCpsServeBcm> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.submit-cps-serve-bcm", SUBMISSION_ID), submitCpsServeBcm);

        stagingProsecutorsCommandHandler.handleServeBcm(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.cps-serve-bcm-received",
                handlerTestHelper.convertFromFile("json/cps-serve-bcm-received.json", JsonObject.class));
    }

    @Test
    public void shouldHandleCpsServePtph() throws IOException, EventStreamException {

        final UUID submissionId = UUID.fromString("7e2f843e-d639-40b3-8611-8015f3a18958");

        final ProsecutionSubmission aggregate = new ProsecutionSubmission();
        final CpsSubmission cpsSubmission = new CpsSubmission();
        final PocaEmailAggregate pocaEmailAggregate = new PocaEmailAggregate();
        when(eventSource.getStreamById(submissionId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CpsSubmission.class)).thenReturn(cpsSubmission);

        final SubmitCpsServePtph submitCpsServePtph =
                handlerTestHelper.convertFromFile("json/cps-serve-ptph.json", SubmitCpsServePtph.class);

        final Envelope<SubmitCpsServePtph> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.submit-cps-serve-ptph", submissionId), submitCpsServePtph);

        stagingProsecutorsCommandHandler.handleServePtph(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.cps-serve-ptph-received",
                handlerTestHelper.convertFromFile("json/cps-serve-ptph-received.json", JsonObject.class));
    }

    @Test
    public void shouldHandleCpsServeCotr() throws IOException, EventStreamException {
        final CpsSubmission cpsSubmission = new CpsSubmission();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CpsSubmission.class)).thenReturn(cpsSubmission);

        final SubmitCpsServeCotr submitCpsServeCotr =
                handlerTestHelper.convertFromFile("json/cps-serve-cotr.json", SubmitCpsServeCotr.class);

        final Envelope<SubmitCpsServeCotr> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.submit-cps-serve-cotr", SUBMISSION_ID), submitCpsServeCotr);

        stagingProsecutorsCommandHandler.handleServeCotr(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.cps-serve-cotr-received",
                handlerTestHelper.convertFromFile("json/cps-serve-cotr-received.json", JsonObject.class));
    }

    @Test
    public void shouldHandleCpsUpdateCotr() throws IOException, EventStreamException {

        final UUID submissionId = UUID.fromString("7e2f843e-d639-40b3-8611-8015f3a18958");

        final CpsSubmission cpsSubmission = new CpsSubmission();
        when(eventSource.getStreamById(submissionId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CpsSubmission.class)).thenReturn(cpsSubmission);

        final SubmitCpsUpdateCotr submitCpsUpdateCotr =
                handlerTestHelper.convertFromFile("json/cps-update-cotr.json", SubmitCpsUpdateCotr.class);

        final Envelope<SubmitCpsUpdateCotr> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.submit-cps-update-cotr", submissionId), submitCpsUpdateCotr);

        stagingProsecutorsCommandHandler.handleUpdateCotr(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.cps-update-cotr-received",
                handlerTestHelper.convertFromFile("json/cps-update-cotr-received.json", JsonObject.class));
    }

    @Test
    public void shouldUpdateSubmissionStatus() throws IOException, EventStreamException {

        final UUID submissionId = UUID.fromString("7e2f843e-d639-40b3-8611-8015f3a18958");

        final CpsSubmission cpsSubmission = new CpsSubmission();
        when(eventSource.getStreamById(submissionId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CpsSubmission.class)).thenReturn(cpsSubmission);

        final UpdateSubmissionStatus updateSubmissionStatus =
                handlerTestHelper.convertFromFile("json/update-submission-status.json", UpdateSubmissionStatus.class);

        final Envelope<UpdateSubmissionStatus> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.update-submission-status", submissionId), updateSubmissionStatus);

        stagingProsecutorsCommandHandler.updateSubmissionStatus(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.submission-status-updated",
                handlerTestHelper.convertFromFile("json/submission-status-updated.json", JsonObject.class));
    }

    @Test
    public void shouldRaiseDocxValidatedEventForIndividual() throws Exception {

        final PocaEmailAggregate pocaEmailAggregate = new PocaEmailAggregate();
        when(eventSource.getStreamById(POCA_MAIL_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, PocaEmailAggregate.class)).thenReturn(pocaEmailAggregate);

        final ReceivePocaEmail receivePocaEmail = getReceivePocaEmail();
        final Envelope<ReceivePocaEmail> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.receive-poca-email", UUID.randomUUID()), receivePocaEmail);

        final ClassLoader classLoader = getClass().getClassLoader();
        final InputStream docXInputStream = Files.newInputStream(Paths.get(classLoader.getResource("docx/iw018-eng-individual-fields.docx").getFile()));

        final Optional<FileReference> fileReference = Optional.of(new FileReference(UUID.randomUUID(), JsonObjects.createObjectBuilder().build(), docXInputStream));

        when(fileRetriever.retrieve(any())).thenReturn(fileReference);

        final CourtApplicationType courtApplicationType = CourtApplicationType.courtApplicationType().build();

        when(referenceDataServiceImpl.retrieveApplicationTypes(any())).thenReturn(Optional.of(courtApplicationType));

        stagingProsecutorsCommandHandler.receivePocaEmail(envelope);

        final JsonNode node = matchEvent(verifyAppendAndGetArgumentFrom(eventStream), "stagingprosecutors.event.poca-document-validated");

        assert node != null;
        final SubmitApplication submitApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(node.toString()), SubmitApplication.class);

        assertCourtApplication(submitApplication, courtApplicationType);
        assertApplicant(submitApplication);

        assertIndividualRespondents(submitApplication, 0, "respondent-");
        assertIndividualRespondents(submitApplication, 1, "respondent2-");
        assertIndividualRespondents(submitApplication, 2, "respondent3-");
        assertIndividualRespondents(submitApplication, 3, "respondent4-");

        assertThat(submitApplication.getBoxHearingRequest().getCourtCentre().getName(), is("court-name-value"));
    }

    @Test
    public void shouldRaiseDocxValidatedEventForOrganisation() throws Exception {
        final PocaEmailAggregate pocaEmailAggregate = new PocaEmailAggregate();
        when(eventSource.getStreamById(POCA_MAIL_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, PocaEmailAggregate.class)).thenReturn(pocaEmailAggregate);

        final ReceivePocaEmail receivePocaEmail = getReceivePocaEmail();
        final Envelope<ReceivePocaEmail> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.receive-poca-email", UUID.randomUUID()), receivePocaEmail);

        final ClassLoader classLoader = getClass().getClassLoader();
        final InputStream docXInputStream = Files.newInputStream(Paths.get(classLoader.getResource("docx/iw018-eng-organisation-fields.docx").getFile()));

        final Optional<FileReference> fileReference = Optional.of(new FileReference(UUID.randomUUID(), JsonObjects.createObjectBuilder().build(), docXInputStream));

        when(fileRetriever.retrieve(any())).thenReturn(fileReference);

        final CourtApplicationType courtApplicationType = CourtApplicationType.courtApplicationType().build();

        when(referenceDataServiceImpl.retrieveApplicationTypes(any())).thenReturn(Optional.of(courtApplicationType));

        stagingProsecutorsCommandHandler.receivePocaEmail(envelope);

        final JsonNode node = matchEvent(verifyAppendAndGetArgumentFrom(eventStream), "stagingprosecutors.event.poca-document-validated");

        assert node != null;
        final SubmitApplication submitApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(node.toString()), SubmitApplication.class);

        assertCourtApplication(submitApplication, courtApplicationType);
        assertApplicant(submitApplication);

        assertOrganisationRespondents(submitApplication, 0, "respondent-");
        assertOrganisationRespondents(submitApplication, 1, "respondent2-");
        assertOrganisationRespondents(submitApplication, 2, "respondent3-");
        assertOrganisationRespondents(submitApplication, 3, "respondent4-");

        assertThat(submitApplication.getBoxHearingRequest().getCourtCentre().getName(), is("court-name-value"));

    }


    @Test
    public void shouldRaiseDocxValidatedEventForMixedIndividualAndOrganisation() throws Exception {
        final ProsecutionSubmission aggregate = new ProsecutionSubmission();
        final CpsSubmission cpsSubmission = new CpsSubmission();
        final PocaEmailAggregate pocaEmailAggregate = new PocaEmailAggregate();
        when(eventSource.getStreamById(POCA_MAIL_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, PocaEmailAggregate.class)).thenReturn(pocaEmailAggregate);

        final ReceivePocaEmail receivePocaEmail = getReceivePocaEmail();
        final Envelope<ReceivePocaEmail> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.receive-poca-email", UUID.randomUUID()), receivePocaEmail);

        final ClassLoader classLoader = getClass().getClassLoader();
        final InputStream docXInputStream = Files.newInputStream(Paths.get(classLoader.getResource("docx/iw018-eng-mixed-respondents-fields.docx").getFile()));

        final Optional<FileReference> fileReference = Optional.of(new FileReference(UUID.randomUUID(), JsonObjects.createObjectBuilder().build(), docXInputStream));

        when(fileRetriever.retrieve(any())).thenReturn(fileReference);

        final CourtApplicationType courtApplicationType = CourtApplicationType.courtApplicationType().build();

        when(referenceDataServiceImpl.retrieveApplicationTypes(any())).thenReturn(Optional.of(courtApplicationType));

        stagingProsecutorsCommandHandler.receivePocaEmail(envelope);

        final JsonNode node = matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.poca-document-validated");

        final SubmitApplication submitApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(node.toString()), SubmitApplication.class);

        assertCourtApplication(submitApplication, courtApplicationType);
        assertApplicant(submitApplication);

        assertIndividualRespondents(submitApplication, 0, "respondent-");
        assertIndividualRespondents(submitApplication, 1, "respondent2-");
        assertOrganisationRespondents(submitApplication, 2, "respondent3-");
        assertOrganisationRespondents(submitApplication, 3, "respondent4-");

        assertThat(submitApplication.getBoxHearingRequest().getCourtCentre().getName(), is("court-name-value"));

    }

    private ReceivePocaEmail getReceivePocaEmail() {
        return receivePocaEmail()
                .withEmailSubject("emailsubject")
                .withPocaEmail("eamail@email.com")
                .withPocaMailId(POCA_MAIL_ID)
                .build();
    }

    private void assertCourtApplication(SubmitApplication submitApplication, CourtApplicationType courtApplicationType) {
        assertThat(submitApplication.getCourtApplication().getId(), is(not(nullValue())));
        assertThat(submitApplication.getCourtApplication().getCourtApplicationType(), is(courtApplicationType));
        assertThat(submitApplication.getCourtApplication().getCourtApplicationCases().get(0).getCaseURN(), is("related-case-urn-value"));
        assertThat(submitApplication.getCourtApplication().getCourtApplicationCases().get(0).getProsecutorOuCode(), is("related-case-prosecutor-ou-code-value"));
    }


    private void assertApplicant(SubmitApplication submitApplication) {
        assertThat(submitApplication.getCourtApplication().getApplicant().getIsSubject(), is(false));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisation().getName(), is("applicant-organisation-name-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getFirstName(), is("applicant-first-name-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getLastName(), is("applicant-last-name-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress1(), is("applicant-address-line1-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress2(), is("applicant-address-line2-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress3(), is("applicant-address-line3-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress4(), is("applicant-address-line4-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress5(), is("applicant-address-line5-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getPostcode(), is("applicant-address-postcode-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getContact().getWork(), is("applicant-contact-number-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getContact().getPrimaryEmail(), is("applicant-email-address-value"));
    }

    private void assertIndividualRespondents(final SubmitApplication submitApplication, int index, final String prefix) {
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getAsn(), is(prefix + "asn-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getCpsDefendantId(), is(prefix + "cps-defendant-id-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getFirstName(), is(prefix + "first-name-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getLastName(), is(prefix + "last-name-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getAddress().getAddress1(), is(prefix + "address-line1-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getAddress().getAddress2(), is(prefix + "address-line2-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getAddress().getAddress3(), is(prefix + "address-line3-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getAddress().getAddress4(), is(prefix + "address-line4-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getAddress().getAddress5(), is(prefix + "address-line5-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getAddress().getPostcode(), is(prefix + "address-postcode-value"));
    }

    private void assertOrganisationRespondents(final SubmitApplication submitApplication, int index, final String prefix) {
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getAsn(), is(prefix + "asn-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getCpsDefendantId(), is(prefix + "cps-defendant-id-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getOrganisation().getName(), is(prefix + "organisation-name-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getOrganisation().getAddress().getAddress1(), is(prefix + "address-line1-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getOrganisation().getAddress().getAddress2(), is(prefix + "address-line2-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getOrganisation().getAddress().getAddress3(), is(prefix + "address-line3-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getOrganisation().getAddress().getAddress4(), is(prefix + "address-line4-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getOrganisation().getAddress().getAddress5(), is(prefix + "address-line5-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getOrganisation().getAddress().getPostcode(), is(prefix + "address-postcode-value"));
    }
}