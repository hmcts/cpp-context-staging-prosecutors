package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class PocaEmailNotificationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PocaEmailNotificationProcessor.class);
    private static final String STAGINGPROSECUTORS_COMMAND_RECIEVE_POCA_EMAIL = "stagingprosecutors.command.receive-poca-email";

    @Inject
    private Sender sender;

    @Handles("public.notificationnotify.events.poca-email-notification-received")
    public void receivePocaEmailNotification(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'public.notificationnotify.events.poca-email-notification-received' received with payload {}", envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(STAGINGPROSECUTORS_COMMAND_RECIEVE_POCA_EMAIL),
                envelope.payloadAsJsonObject()));

    }
}
