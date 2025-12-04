package uk.gov.moj.cpp.staging.prosecutors.command.handler;

import static com.google.common.collect.ImmutableList.of;
import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialPendingWithWarnings.materialPendingWithWarnings;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem.problem;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.RejectSubmission.rejectSubmission;
import static uk.gov.moj.cpp.staging.prosecutors.test.utils.HandlerTestHelper.matchEvent;
import static uk.gov.moj.cpp.staging.prosecutors.test.utils.HandlerTestHelper.metadataFor;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.staging.prosecutors.domain.MaterialSubmission;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialPendingWithWarnings;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveMaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.RejectSubmission;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionPendingWithWarnings;
import uk.gov.moj.cpp.staging.prosecutors.test.utils.FileResourceObjectMapper;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonValue;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.MaterialSubmittedV3;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmitMaterialCommand;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmitMaterialCommandV3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsMaterialCommandHandlerTest {

    private static final UUID SUBMISSION_ID = fromString("8191c165-e9e4-4fd7-ac6d-5bfd04690f77");

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            MaterialSubmitted.class,
            MaterialSubmissionRejected.class,
            MaterialSubmissionSuccessful.class,
            MaterialSubmittedV3.class,
            SubmissionPendingWithWarnings.class);

    @InjectMocks
    private StagingProsecutorsMaterialCommandHandler stagingProsecutorsMaterialCommandHandler;

    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    @Test
    public void shouldHandleSubmitMaterialCommand() {
        assertThat(new StagingProsecutorsMaterialCommandHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleSubmitMaterial")
                        .thatHandles("stagingprosecutors.command.submit-material")
                ));
    }

    @Test
    public void shouldHandlehandleSubmitMaterialCpsCommand() {
        assertThat(new StagingProsecutorsMaterialCommandHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleSubmitCpsMaterial")
                        .thatHandles("stagingprosecutors.command.submit-cps-material")
                ));
    }

    @Test
    public void shouldHandleMaterialSubmission() throws IOException, EventStreamException {

        final MaterialSubmission aggregate = new MaterialSubmission();
        when(eventSource.getStreamById(SUBMISSION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialSubmission.class)).thenReturn(aggregate);

        final SubmitMaterialCommand materialSubmission =
                handlerTestHelper.convertFromFile("json/submitMaterial.json", SubmitMaterialCommand.class);

        final Envelope<SubmitMaterialCommand> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.submit-material", SUBMISSION_ID), materialSubmission);
        stagingProsecutorsMaterialCommandHandler.handleSubmitMaterial(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.material-submitted",
                handlerTestHelper.convertFromFile("json/material_submitted.json", JsonValue.class));
    }


    @Test
    public void shouldHandleReceiveMaterialSubmissionSuccessful() throws IOException, EventStreamException {

        final MaterialSubmission aggregate = new MaterialSubmission();
        when(eventSource.getStreamById(SUBMISSION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialSubmission.class)).thenReturn(aggregate);

        final ReceiveMaterialSubmissionSuccessful materialSubmission =
                handlerTestHelper.convertFromFile("json/receiveMaterialSubmissionSuccessful.json", ReceiveMaterialSubmissionSuccessful.class);

        final Envelope<ReceiveMaterialSubmissionSuccessful> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.receive-material-submissions-successful", SUBMISSION_ID), materialSubmission);
        stagingProsecutorsMaterialCommandHandler.handleReceiveMaterialSubmissionSuccessful(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.material-submission-successful",
                handlerTestHelper.convertFromFile("json/materialSubmissionSuccessful.json", JsonValue.class));
    }

    @Test
    public void shouldHandleMaterialSubmissionRejected() throws Exception {
        final MaterialSubmission aggregate = new MaterialSubmission();
        when(eventSource.getStreamById(SUBMISSION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialSubmission.class)).thenReturn(aggregate);
        final Metadata metadata = metadataFor("stagingprosecutors.command.reject-submission", SUBMISSION_ID);

        final Problem problem = problem()
                .withCode("INVALID_DOCUMENT_TYPE")
                .withValues(of(problemValue().withKey("documentType").withValue("PLEA").build()))
                .build();

        final RejectSubmission rejectSubmission = rejectSubmission()
                .withSubmissionId(SUBMISSION_ID)
                .withErrors(of(problem))
                .build();

        final Envelope<RejectSubmission> envelope = envelopeFrom(metadata, rejectSubmission);

        stagingProsecutorsMaterialCommandHandler.handleReceiveMaterialSubmissionRejected(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.material-submission-rejected",
                handlerTestHelper.convertFromFile("json/materialSubmissionRejected.json", JsonValue.class));
    }

    @Test
    public void shouldHandleMaterialSubmissionV3() throws IOException, EventStreamException {

        final MaterialSubmission aggregate = new MaterialSubmission();
        when(eventSource.getStreamById(SUBMISSION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialSubmission.class)).thenReturn(aggregate);
        final SubmitMaterialCommandV3 submitMaterialCommandV3 =
                handlerTestHelper.convertFromFile("json/submitMaterial_v3.json", SubmitMaterialCommandV3.class);

        final Envelope<SubmitMaterialCommandV3> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.submit-material-v3", SUBMISSION_ID), submitMaterialCommandV3);
        stagingProsecutorsMaterialCommandHandler.handleSubmitMaterialV3(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.material-submitted-v3",
                handlerTestHelper.convertFromFile("json/material_submitted_v3.json", JsonValue.class));
    }

    @Test
    public void shouldHandleMaterialSubmissionPendingWithWarnings() throws Exception {
        final MaterialSubmission aggregate = new MaterialSubmission();
        when(eventSource.getStreamById(SUBMISSION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialSubmission.class)).thenReturn(aggregate);
        final Metadata metadata = metadataFor("stagingprosecutors.command.material-pending-with-warnings", SUBMISSION_ID);

        final Problem problem = problem()
                .withCode("DEFENDANT_ON_CP")
                .withValues(of(problemValue().withKey("defendant").withValue("NOT IN CP").build()))
                .build();

        final MaterialPendingWithWarnings materialPendingWithWarnings = materialPendingWithWarnings()
                .withSubmissionId(SUBMISSION_ID)
                .withWarnings(of(problem))
                .build();

        final Envelope<MaterialPendingWithWarnings> envelope = envelopeFrom(metadata, materialPendingWithWarnings);

        stagingProsecutorsMaterialCommandHandler.handleMaterialSubmissionPendingWithWarnings(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.submission-pending-with-warnings",
                handlerTestHelper.convertFromFile("json/materialSubmissionPendingWithWarnings.json", JsonValue.class));
    }

}