package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.util.ApplicationParameters;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PocaDocumentValidationProcessorTest {

    @InjectMocks
    private PocaDocumentValidationProcessor pocaDocumentValidationProcessor;

    @Mock
    private Sender sender;
    @Mock
    private ApplicationParameters applicationParameters;
    @Captor
    private ArgumentCaptor<Envelope> captor;

    @Test
    public void shouldHandleStagingprosecutorsEventPocaDocumentValidatedEvent() {
        assertThat(pocaDocumentValidationProcessor, isHandler(EVENT_PROCESSOR)
                .with(method("pocaDocumentValidated")
                        .thatHandles("stagingprosecutors.event.poca-document-validated")
                ));
    }

    @Test
    public void shouldHandleStagingprosecutorsEventPocaDocumentNotValidatedEvent() {
        assertThat(pocaDocumentValidationProcessor, isHandler(EVENT_PROCESSOR)
                .with(method("pocaDocumentNotValidated")
                        .thatHandles("stagingprosecutors.event.poca-document-not-validated")
                ));
    }

    @Test
    public void shouldTiggerCommandWhenStagingprosecutorsEventPocaDocumentValidatedEventReceived() {

        final JsonObject eventPayload = createObjectBuilder()
                .add("emailSubject", "emailSubject")
                .add("pocaFileId", randomUUID().toString())
                .add("senderEmail", "senderEmail@hmcts.net")
                .build();

        final Metadata metadata = metadataBuilder()
                .withName("stagingprosecutors.event.poca-document-validated")
                .withId(randomUUID())
                .build();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, eventPayload);

        pocaDocumentValidationProcessor.pocaDocumentValidated(envelope);

        verify(sender).send(captor.capture());

        final Envelope defaultEnvelope = captor.getValue();
        assertThat(defaultEnvelope.metadata().name(), is("stagingprosecutors.command.submit-application"));
    }

    @Test
    public void shouldSendEmailNotificationWhenStagingprosecutorsEventPocaDocumentNotValidatedEventReceived() {

        final JsonObject eventPayload = createObjectBuilder()
                .add("emailSubject", "emailSubject")
                .add("errors", JsonObjects.createArrayBuilder().add(createObjectBuilder().add("errorCode", "errorCode1")))
                .add("senderEmail", "senderEmail@hmcts.net")
                .build();

        final Metadata metadata = metadataBuilder()
                .withName("stagingprosecutors.event.poca-document-not-validated")
                .withId(randomUUID())
                .build();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, eventPayload);

        when(applicationParameters.getEmailTemplateId(any())).thenReturn(randomUUID().toString());

        pocaDocumentValidationProcessor.pocaDocumentNotValidated(envelope);

        verify(sender).sendAsAdmin(captor.capture());

        final Envelope defaultEnvelope = captor.getValue();
        assertThat(defaultEnvelope.metadata().name(), is("notificationnotify.send-email-notification"));
    }
}
