package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.json.schema.event.PublicCaseDocumentUploaded;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveMaterialSubmissionSuccessful;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveMaterialSubmissionSuccessful.receiveMaterialSubmissionSuccessful;

@ServiceComponent(EVENT_PROCESSOR)
public class SjpPublicEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SjpPublicEventProcessor.class);

    @Inject
    private Sender sender;

    @Handles("public.sjp.case-document-uploaded")
    public void caseDocumentUploaded(final Envelope<PublicCaseDocumentUploaded> caseDocumentUploadedEnvelope) {
        final JsonObject metadataJson = caseDocumentUploadedEnvelope.metadata().asJsonObject();

        final Optional<UUID> submissionId = Optional.ofNullable(caseDocumentUploadedEnvelope.metadata().asJsonObject().getString("submissionId", null)).map(UUID::fromString);
        if (submissionId.isPresent()) {
            final ReceiveMaterialSubmissionSuccessful command = receiveMaterialSubmissionSuccessful()
                    .withSubmissionId(submissionId.get())
                    .build();

            final JsonEnvelope envelopedJsonEnvelope = envelopeFrom(metadataFrom(caseDocumentUploadedEnvelope.metadata())
                            .withName("stagingprosecutors.command.receive-material-submission-successful"), createObjectBuilder().build());

            sender.send(envelopeFrom(
                    metadataFrom(envelopedJsonEnvelope.metadata()),
                    command));
        } else {
            LOGGER.debug("Received CaseDocumentUploaded event with no submissionId[Metadata: {}], [DocumentId: {}]",
                    metadataJson, caseDocumentUploadedEnvelope.payload().getDocumentId());
        }
    }
}
