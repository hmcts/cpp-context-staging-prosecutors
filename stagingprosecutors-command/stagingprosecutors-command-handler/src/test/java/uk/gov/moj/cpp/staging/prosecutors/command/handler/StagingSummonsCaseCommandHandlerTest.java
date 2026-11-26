package uk.gov.moj.cpp.staging.prosecutors.command.handler;

import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.moj.cpp.staging.prosecutors.test.utils.HandlerTestHelper.matchEvent;
import static uk.gov.moj.cpp.staging.prosecutors.test.utils.HandlerTestHelper.metadataFor;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.staging.prosecutors.domain.ProsecutionSubmission;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.test.utils.FileResourceObjectMapper;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingSummonsCaseCommandHandlerTest {

    private static final UUID SUBMISSION_ID = UUID.fromString("8191c165-e9e4-4fd7-ac6d-5bfd04690f77");

    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            ProsecutionReceived.class,
            SubmissionRejected.class);

    @InjectMocks
    SubmitSummonsCaseCommandHandler stagingSummonsCaseCommandHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    private ProsecutionSubmission aggregate;

    @BeforeEach
    public void setup() {
        aggregate = new ProsecutionSubmission();
        when(eventSource.getStreamById(SUBMISSION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionSubmission.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleSummonsCaseSubmission() throws IOException, EventStreamException {

        final SubmitSummonsProsecution submitSummonsProsecution =
                handlerTestHelper.convertFromFile("json/submitSummonsProsecution.json", SubmitSummonsProsecution.class);

        final Envelope<SubmitSummonsProsecution> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.summons-prosecution", SUBMISSION_ID), submitSummonsProsecution);
        stagingSummonsCaseCommandHandler.handleSummonsProsecutionSubmission(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.prosecution-received",
                handlerTestHelper.convertFromFile("json/summons_prosecution_received.json", JsonValue.class));
    }


}