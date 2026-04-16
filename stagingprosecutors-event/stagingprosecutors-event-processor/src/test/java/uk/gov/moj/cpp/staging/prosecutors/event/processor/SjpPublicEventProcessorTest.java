package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import uk.gov.justice.services.messaging.JsonObjects;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.sjp.json.schema.event.PublicCaseDocumentUploaded;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveMaterialSubmissionSuccessful;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SjpPublicEventProcessorTest {

    private static final UUID SUBMISSION_ID = randomUUID();

    @Mock
    private Sender sender;

    @InjectMocks
    private SjpPublicEventProcessor sjpPublicEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<ReceiveMaterialSubmissionSuccessful>> materialSubmissionCaptor;

    @Test
    public void shouldHandlePublicSjpCaseCreatedEvent() {
        assertThat(sjpPublicEventProcessor, isHandler(EVENT_PROCESSOR)
                .with(method("caseDocumentUploaded")
                        .thatHandles("public.sjp.case-document-uploaded")
                ));
    }

    @Test
    public void shouldSendReceiveMaterialSubmissionSuccessfulCommand() {

        final UUID caseId = randomUUID();
        final UUID documentId = randomUUID();
        final PublicCaseDocumentUploaded caseDocumentUploaded = PublicCaseDocumentUploaded.publicCaseDocumentUploaded()
                .withCaseId(caseId)
                .withDocumentId(documentId)
                .build();

        final Envelope<PublicCaseDocumentUploaded> caseDocumentUploadedEnvelope = testEnvelope(caseDocumentUploaded, "public.sjp.case-document-created");

        sjpPublicEventProcessor.caseDocumentUploaded(caseDocumentUploadedEnvelope);

        verify(sender).send(materialSubmissionCaptor.capture());

        final Envelope<ReceiveMaterialSubmissionSuccessful> envelope = materialSubmissionCaptor.getValue();

        final Metadata metadata = envelope.metadata();
        final ReceiveMaterialSubmissionSuccessful receiveMaterialSubmissionSuccessful = envelope.payload();

        assertThat(metadata.streamId(), is(caseDocumentUploadedEnvelope.metadata().streamId()));
        assertThat(metadata.name(), is("stagingprosecutors.command.receive-material-submission-successful"));

        assertThat(receiveMaterialSubmissionSuccessful.getSubmissionId().toString(), is(SUBMISSION_ID.toString()));
    }

    private <T extends Object> Envelope<T> testEnvelope(final T payload, final String eventName) {

        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();
        final UUID streamId = randomUUID();
        final UUID id = randomUUID();

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(id)
                .withName(eventName)
                .withSessionId(sessionId.toString())
                .withUserId(userId.toString())
                .withStreamId(streamId);

        return Envelope.envelopeFrom(metadataFrom(uk.gov.justice.services.test.utils.core.messaging.JsonObjects.createObjectBuilder(
                metadataBuilder.build().asJsonObject()).add("submissionId", SUBMISSION_ID.toString()).build())
                .withUserId(userId.toString()).build(), payload);

    }

}