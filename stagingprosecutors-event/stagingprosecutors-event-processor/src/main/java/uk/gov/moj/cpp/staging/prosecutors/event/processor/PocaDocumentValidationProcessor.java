package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.util.ApplicationParameters;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class PocaDocumentValidationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PocaDocumentValidationProcessor.class);
    private static final String STAGINGPROSECUTORS_COMMAND_SUBMIT_APPLICATION = "stagingprosecutors.command.submit-application";
    private static final String NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE = "notificationnotify.send-email-notification";
    private static final String FIELD_TEMPLATE_ID = "templateId";
    private static final String SEND_TO_ADDRESS = "sendToAddress";
    private static final String PERSONALISATION = "personalisation";
    private static final String SUBJECT = "subject";
    private static final String FIELD_NOTIFICATION_ID = "notificationId";

    @Inject
    private Sender sender;

    @Inject
    private ApplicationParameters applicationParameters;

    @Handles("stagingprosecutors.event.poca-document-validated")
    public void pocaDocumentValidated(final JsonEnvelope envelope) {
        LOGGER.info("'stagingprosecutors.event.poca-document-validated' received with payload {}", envelope.toObfuscatedDebugString());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'stagingprosecutors.event.poca-document-validated' received with payload {}", envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(STAGINGPROSECUTORS_COMMAND_SUBMIT_APPLICATION),
                envelope.payloadAsJsonObject()));

    }

    @Handles("stagingprosecutors.event.poca-document-not-validated")
    public void pocaDocumentNotValidated(final JsonEnvelope envelope) {
        LOGGER.info("'stagingprosecutors.event.poca-document-not-validated' received with payload {}", envelope.toObfuscatedDebugString());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'stagingprosecutors.event.poca-document-not-validated' received with payload {}", envelope.toObfuscatedDebugString());
        }
        final JsonObject eventPayload = envelope.payloadAsJsonObject();

        final JsonArray errors = eventPayload.getJsonArray("errors");
        final String senderEmail = eventPayload.getString("senderEmail");
        final String emailSubject = eventPayload.getString("emailSubject");

        errors.forEach(error -> sendEmailNotification(envelope, (JsonObject) error, senderEmail, emailSubject));

    }

    public void sendEmailNotification(final JsonEnvelope jsonEnvelope, final JsonObject emailNotification, final String senderEmail, final String emailSubject) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("sending email notification - {} ", emailNotification);
        }

        final JsonObjectBuilder notifyObjectBuilder = createObjectBuilder();
        notifyObjectBuilder.add(FIELD_NOTIFICATION_ID, UUID.randomUUID().toString());
        notifyObjectBuilder.add(FIELD_TEMPLATE_ID, applicationParameters.getEmailTemplateId(emailNotification.getString("errorCode")));
        notifyObjectBuilder.add(SEND_TO_ADDRESS, senderEmail);

        final JsonObjectBuilder personalisationObjectBuilder = createObjectBuilder();
        personalisationObjectBuilder.add(SUBJECT, emailSubject);
        notifyObjectBuilder.add(PERSONALISATION, personalisationObjectBuilder.build());

        sender.sendAsAdmin(envelopeFrom(
                        metadataFrom(jsonEnvelope.metadata()).withName(NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE),
                        notifyObjectBuilder.build()
                )
        );
    }
}
