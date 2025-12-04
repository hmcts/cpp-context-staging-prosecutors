package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cps.stagingprosecutors.domain.event.ProsecutionSubmissionSucceededWithWarnings.prosecutionSubmissionSucceededWithWarnings;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSubmissionSuccessfulWithWarnings;
import uk.gov.moj.cps.stagingprosecutors.domain.event.ProsecutionSubmissionSucceededWithWarnings;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionSubmissionSucceededWithWarningsPublicEventProcessorTest {

    @Mock
    private Sender sender;


    @InjectMocks
    private ProsecutionSubmissionSucceededWithWarningsPublicEventProcessor prosecutionSubmissionSucceededWithWarningsPublicEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<ReceiveSubmissionSuccessfulWithWarnings>> captor;


    @Test
    public void shouldHandleProsecutionSubmissionSucceededWithWarningsEvent() {
        assertThat(prosecutionSubmissionSucceededWithWarningsPublicEventProcessor, isHandler(EVENT_PROCESSOR)
                .with(method("prosecutionSubmissionSucceededWithWarnings")
                        .thatHandles("public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings")
                ));
    }

    @Test
    public void shouldSendReceiveSubmissionSuccessfulCommand() {

        ProsecutionSubmissionSucceededWithWarnings prosecutionSubmissionSucceededWithWarnings = prosecutionSubmissionSucceededWithWarningsBuilder().build();
        final Envelope<ProsecutionSubmissionSucceededWithWarnings> prosecutionSubmissionSucceededEnvelope
                = Envelope.envelopeFrom(getMetadata(), prosecutionSubmissionSucceededWithWarnings);
        whenTheProcessorHandlesTheEvent(prosecutionSubmissionSucceededEnvelope);

        thenProsecutionSuccessfulSucceededWithWarningsCommandIsRaisedCorrectly(prosecutionSubmissionSucceededEnvelope);
    }

    @Test
    public void shouldNotSendReceiveSubmissionSuccessfulCommandWhenCaseCreatedHasNoSubmissionId() {

        ProsecutionSubmissionSucceededWithWarnings prosecutionSubmissionSucceededWithWarnings
                = prosecutionSubmissionSucceededWithWarningsBuilder().withExternalId(null).build();
        final Envelope<ProsecutionSubmissionSucceededWithWarnings> prosecutionSubmissionSucceededEnvelope
                = Envelope.envelopeFrom(getMetadata(), prosecutionSubmissionSucceededWithWarnings);
        whenTheProcessorHandlesTheEvent(prosecutionSubmissionSucceededEnvelope);

        verifyNoInteractions(sender);
    }

    @Test
    public void shouldNotSendReceiveSubmissionSuccessfulCommandWhenChannelIsNotCPPI() {

        ProsecutionSubmissionSucceededWithWarnings prosecutionSubmissionSucceededWithWarnings
                = prosecutionSubmissionSucceededWithWarningsBuilder().withChannel(SPI).build();
        final Envelope<ProsecutionSubmissionSucceededWithWarnings> prosecutionSubmissionSucceededEnvelope
                = Envelope.envelopeFrom(getMetadata(), prosecutionSubmissionSucceededWithWarnings);
        whenTheProcessorHandlesTheEvent(prosecutionSubmissionSucceededEnvelope);

        verifyNoInteractions(sender);
    }

    private ProsecutionSubmissionSucceededWithWarnings.Builder prosecutionSubmissionSucceededWithWarningsBuilder() {

        final Problem warning = Problem.problem()
                .withCode("problemCode")
                .build();
       return prosecutionSubmissionSucceededWithWarnings()
                .withCaseId(randomUUID())
                .withWarnings(ImmutableList.of(warning))
                .withExternalId(randomUUID())
                .withChannel(CPPI);
    }

    private Metadata getMetadata() {
        return metadataBuilder()
                .withId(randomUUID())
                .withName("public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings")
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(randomUUID())
                .build();
    }

    private void whenTheProcessorHandlesTheEvent(final Envelope<ProsecutionSubmissionSucceededWithWarnings> prosecutionSubmissionSucceededWithWarningsEnvelope) {
        prosecutionSubmissionSucceededWithWarningsPublicEventProcessor.prosecutionSubmissionSucceededWithWarnings(prosecutionSubmissionSucceededWithWarningsEnvelope);
    }

    private void thenProsecutionSuccessfulSucceededWithWarningsCommandIsRaisedCorrectly(final Envelope<ProsecutionSubmissionSucceededWithWarnings> prosecutionSubmissionSucceededWithWarningsEnvelope) {
        verify(sender).send(captor.capture());

        final Envelope<ReceiveSubmissionSuccessfulWithWarnings> envelope = captor.getValue();

        final Metadata metadata = envelope.metadata();
        final ReceiveSubmissionSuccessfulWithWarnings receiveSubmissionSuccessfulWithWarnings = envelope.payload();

        assertThat(metadata.streamId(), is(prosecutionSubmissionSucceededWithWarningsEnvelope.metadata().streamId()));
        assertThat(metadata.name(), is("stagingprosecutors.command.receive-submission-successful-with-warnings"));
        assertThat(receiveSubmissionSuccessfulWithWarnings.getSubmissionId(), is(prosecutionSubmissionSucceededWithWarningsEnvelope.payload().getExternalId()));
    }
}