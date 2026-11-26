package uk.gov.moj.cpp.staging.prosecutors.command.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.staging.prosecutors.test.utils.HandlerTestHelper.matchEvent;
import static uk.gov.moj.cpp.staging.prosecutors.test.utils.HandlerTestHelper.metadataFor;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.staging.prosecutors.domain.ApplicationSubmission;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ApplicationSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.test.utils.FileResourceObjectMapper;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonValue;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.command.SubmitApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsApplicationCommandHandlerTest {

    private static final UUID SUBMISSION_ID = UUID.fromString("a5808a9f-b2f0-455b-b030-baeaad907dcb");

    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            ApplicationSubmitted.class);
    @InjectMocks
    StagingProsecutorsApplicationCommandHandler stagingProsecutorsApplicationCommandHandler;
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;

    @Test
    public void shouldHandlerReceiveAllegationsCommand() {
        assertThat(new StagingProsecutorsApplicationCommandHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleSubmitApplication")
                        .thatHandles("stagingprosecutors.command.submit-application")
                ));
    }

    @Test
    public void shouldHandleSubmitApplication() throws IOException, EventStreamException {

        final ApplicationSubmission applicationAggregate = new ApplicationSubmission();
        when(eventSource.getStreamById(SUBMISSION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationSubmission.class)).thenReturn(applicationAggregate);
        final SubmitApplication submitApplication =
                handlerTestHelper.convertFromFile("json/submitApplication.json", SubmitApplication.class);

        final Envelope<SubmitApplication> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.submit-application"), submitApplication);
        stagingProsecutorsApplicationCommandHandler.handleSubmitApplication(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "stagingprosecutors.event.application-submitted",
                handlerTestHelper.convertFromFile("json/applicationSubmitted.json", JsonValue.class));
    }
}