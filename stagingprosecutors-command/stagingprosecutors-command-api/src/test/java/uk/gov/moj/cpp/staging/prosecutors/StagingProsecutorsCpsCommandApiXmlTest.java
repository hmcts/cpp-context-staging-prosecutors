package uk.gov.moj.cpp.staging.prosecutors;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.converter.MediaTypeResolver;
import uk.gov.moj.cpp.staging.prosecutors.converter.SubmitCpsMaterialConverter;

import java.io.File;
import java.io.IOException;

import javax.json.JsonObject;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmitCpsMaterialCommand;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsCpsCommandApiXmlTest {

    public static final String INVALID_XML_PAYLOAD = "<test></test>";

    @Spy
    private SubmitCpsMaterialConverter submitCpsMaterialConverter;

    @Spy
    private MediaTypeResolver mediaTypeResolver;

    @Mock
    private Sender sender;

    @Mock
    private FileStorer fileStorer;

    @Captor
    private ArgumentCaptor<Envelope<SubmitCpsMaterialCommand>> cpsMaterialEnvelopeCaptor;

    @InjectMocks
    private StagingProsecutorsCpsCommandApiXml stagingProsecutorsCpsCommandApiXml;

    @Test
    public void shouldHandleSubmitCpsMaterial() throws IOException {
        final String xmlPayload = FileUtils.readFileToString(new File(Thread.currentThread().getContextClassLoader().getResource("CP20.xml").getFile()));
        final String fileStoreIds = randomUUID().toString() + ","+ randomUUID().toString();
        final JsonObject payload = createObjectBuilder()
                .add("payload", xmlPayload)
                .add("fileStoreIds",fileStoreIds)
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("hmcts.cjs.cps-submit-material", payload);
        final Envelope<JsonObject> submitMaterialResponseEnvelope = stagingProsecutorsCpsCommandApiXml.submitMaterial(requestEnvelope);

        assertThat(submitMaterialResponseEnvelope.payload().getString("submissionId"), is(notNullValue()));

        verify(sender).send(cpsMaterialEnvelopeCaptor.capture());

        final Envelope<SubmitCpsMaterialCommand> envelopeCaptorValue = cpsMaterialEnvelopeCaptor.getValue();
        assertThat(envelopeCaptorValue.metadata().name(), is("stagingprosecutors.command.submit-cps-material"));
        assertThat(envelopeCaptorValue.payload().getDefendants().size(), is(2));
        assertThat(envelopeCaptorValue.payload().getDocuments().size(), is(2));
    }

    @Test
    public void shouldThrowBadRequestExceptionWhenXmlPayloadIsInvalid() {

        final String fileStoreIds = "";
        final JsonObject payload = createObjectBuilder()
                .add("payload", INVALID_XML_PAYLOAD)
                .add("fileStoreIds",fileStoreIds)
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("hmcts.cjs.cps-submit-material", payload);
        assertThrows(BadRequestException.class, () -> stagingProsecutorsCpsCommandApiXml.submitMaterial(requestEnvelope));
    }
}
