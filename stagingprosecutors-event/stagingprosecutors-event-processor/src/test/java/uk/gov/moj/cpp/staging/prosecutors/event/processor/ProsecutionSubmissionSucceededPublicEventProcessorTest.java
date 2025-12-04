package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSubmissionSuccessful;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceeded;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionSubmissionSucceededPublicEventProcessorTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private ProsecutionSubmissionSucceededPublicEventProcessor prosecutionSubmissionSucceededProcessor;

    @Captor
    private ArgumentCaptor<Envelope<ReceiveSubmissionSuccessful>> captor;


    @Test
    public void shouldHandleProsecutionSubmissionSucceededEvent() {
        assertThat(prosecutionSubmissionSucceededProcessor, isHandler(EVENT_PROCESSOR)
                .with(method("prosecutionSubmissionSucceeded")
                        .thatHandles("public.prosecutioncasefile.prosecution-submission-succeeded")
                ));
    }

    @Test
    public void shouldSendReceiveSubmissionSuccessfulCommand() {

        ProsecutionSubmissionSucceeded prosecutionSubmissionSucceeded = prosecutionSubmissionSucceededBuilder().build();
        final Envelope<ProsecutionSubmissionSucceeded> prosecutionSubmissionSucceededEnvelope
                = Envelope.envelopeFrom(getMetadata(), prosecutionSubmissionSucceeded);
        whenTheProcessorHandlesTheEvent(prosecutionSubmissionSucceededEnvelope);

        thenProsecutionSuccessfulSucceededCommandIsRaisedCorrectly(prosecutionSubmissionSucceededEnvelope);
    }

    @Test
    public void shouldNotSendReceiveSubmissionSuccessfulCommandWhenCaseCreatedHasNoSubmissionId() {

        ProsecutionSubmissionSucceeded prosecutionSubmissionSucceeded = prosecutionSubmissionSucceededBuilder().withExternalId(null).build();
        final Envelope<ProsecutionSubmissionSucceeded> prosecutionSubmissionSucceededEnvelope
                = Envelope.envelopeFrom(getMetadata(), prosecutionSubmissionSucceeded);
        whenTheProcessorHandlesTheEvent(prosecutionSubmissionSucceededEnvelope);

        verifyNoInteractions(sender);
    }

    @Test
    public void shouldNotSendReceiveSubmissionSuccessfulCommandWhenChannelIsSPI() {

        ProsecutionSubmissionSucceeded prosecutionSubmissionSucceeded = prosecutionSubmissionSucceededBuilder().withChannel(SPI).build();
        final Envelope<ProsecutionSubmissionSucceeded> prosecutionSubmissionSucceededEnvelope
                = Envelope.envelopeFrom(getMetadata(), prosecutionSubmissionSucceeded);
        whenTheProcessorHandlesTheEvent(prosecutionSubmissionSucceededEnvelope);

        verifyNoInteractions(sender);
    }

    private ProsecutionSubmissionSucceeded.Builder prosecutionSubmissionSucceededBuilder() {
        return ProsecutionSubmissionSucceeded.prosecutionSubmissionSucceeded()
                .withCaseId(randomUUID())
                .withChannel(CPPI)
                .withExternalId(randomUUID());
    }

    private Metadata getMetadata() {
        return metadataBuilder()
                .withId(randomUUID())
                .withName("public.prosecutioncasefile.prosecution-submission-succeeded")
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(randomUUID())
                .build();
    }

    private void whenTheProcessorHandlesTheEvent(final Envelope<ProsecutionSubmissionSucceeded> prosecutionSubmissionSucceededEnvelope) {
        prosecutionSubmissionSucceededProcessor.prosecutionSubmissionSucceeded(prosecutionSubmissionSucceededEnvelope);
    }

    private void thenProsecutionSuccessfulSucceededCommandIsRaisedCorrectly(final Envelope<ProsecutionSubmissionSucceeded> prosecutionSubmissionSucceededEnvelope) {
        verify(sender).send(captor.capture());

        final Envelope<ReceiveSubmissionSuccessful> envelope = captor.getValue();

        final Metadata metadata = envelope.metadata();
        final ReceiveSubmissionSuccessful receiveSubmissionSuccessful = envelope.payload();

        assertThat(metadata.streamId(), is(prosecutionSubmissionSucceededEnvelope.metadata().streamId()));
        assertThat(metadata.name(), is("stagingprosecutors.command.receive-submission-successful"));
        assertThat(receiveSubmissionSuccessful.getSubmissionId(), is(prosecutionSubmissionSucceededEnvelope.payload().getExternalId()));
    }
}