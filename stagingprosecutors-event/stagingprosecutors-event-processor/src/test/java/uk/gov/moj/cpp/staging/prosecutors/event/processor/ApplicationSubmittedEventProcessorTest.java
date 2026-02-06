package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ApplicationSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.unbundling.utility.FileUtil;

import java.io.StringReader;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationSubmittedEventProcessorTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private ApplicationSubmittedEventProcessor applicationSubmittedEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<ApplicationSubmitted>> applicationSubmittedCaptor;

    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());


    @Test
    public void shouldHandlePublicSjpCaseCreatedEvent() {
        assertThat(applicationSubmittedEventProcessor, isHandler(EVENT_PROCESSOR)
                .with(method("applicationSubmitted")
                        .thatHandles("stagingprosecutors.event.application-submitted")
                ));
    }

    @Test
    public void shouldSendProsecutionCaseFileSubmitApplicationCommand() {
        final Metadata metadataJsonObject = metadataFrom(
                JsonObjects.createObjectBuilder(metadataWithRandomUUID("stagingprosecutors.event.application-submitted").build().asJsonObject())
                        .build())
                .build();

        final ApplicationSubmitted applicationSubmittedEventPayload = jsonObjectToObjectConverter.convert(getResponsePayload("stagingprosecutors.event.application-submitted.json"), ApplicationSubmitted.class);

        final Envelope<ApplicationSubmitted> caseDocumentUploadedEnvelope = Envelope.envelopeFrom(metadataJsonObject, applicationSubmittedEventPayload);
        applicationSubmittedEventProcessor.applicationSubmitted(caseDocumentUploadedEnvelope);

        verify(sender).sendAsAdmin(applicationSubmittedCaptor.capture());

        final Envelope<ApplicationSubmitted> commandEnvelope = applicationSubmittedCaptor.getValue();

        final Metadata metadata = commandEnvelope.metadata();
        final ApplicationSubmitted applicationSubmittedCommandPayload = commandEnvelope.payload();

        assertThat(metadata.name(), is("prosecutioncasefile.command.submit-application"));
        assertThat(applicationSubmittedCommandPayload, is(applicationSubmittedEventPayload));

    }

    private JsonObject getResponsePayload(final String filePath) {
        return createReader(new StringReader(FileUtil.getPayload(filePath))).readObject();
    }

}