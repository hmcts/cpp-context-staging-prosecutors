package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.unbundling.utility.FileUtil;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PocaEmailNotificationProcessorTest {

    @InjectMocks
    private PocaEmailNotificationProcessor pocaEmailNotificationProcessor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> captor;

    @Test
    public void shouldHandlePublicNotofocationNotifyPocaEmailReceivedEvent() {
        assertThat(pocaEmailNotificationProcessor, isHandler(EVENT_PROCESSOR)
                .with(method("receivePocaEmailNotification")
                        .thatHandles("public.notificationnotify.events.poca-email-notification-received")
                ));
    }

    @Test
    public void shouldReceivePocaEmailNotofication() {

        final JsonObject eventPayload = FileUtil.givenPayload("/public.notificationnotify.poca-email-notification-received.json");

        final Metadata metadata = metadataBuilder()
                .withName("public.notificationnotify.events.poca-email-notification-received")
                .withId(UUID.randomUUID())
                .build();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, eventPayload);

        pocaEmailNotificationProcessor.receivePocaEmailNotification(envelope);

        verify(sender).send(captor.capture());

        final JsonEnvelope defaultEnvelope = captor.getValue();
        assertThat(defaultEnvelope.metadata().name(), is("stagingprosecutors.command.receive-poca-email"));
    }
}
