package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.converter.ProsecutionRejectedToRejectSubmissionConverter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.RejectSubmission;
import uk.gov.moj.cps.stagingprosecutors.domain.event.PublicProsecutionRejected;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionRejectedProcessor {

    static final String REJECT_SUBMISSION_COMMAND = "stagingprosecutors.command.reject-submission";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionRejectedProcessor.class);

    @Inject
    private Sender sender;


    @Handles("public.prosecutioncasefile.prosecution-rejected")
    public void onProsecutionRejected(final Envelope<PublicProsecutionRejected> prosecutionRejected) {

        final PublicProsecutionRejected payload = prosecutionRejected.payload();
        final Optional<UUID> externalId = ofNullable(payload.getExternalId());

        if (!externalId.isPresent() || !CPPI.equals(payload.getChannel())) {
            LOGGER.info("Message unrelated to CPPI channel for rejecting case.  Not processing");
            return;
        }

        final ProsecutionRejectedToRejectSubmissionConverter converter =
                new ProsecutionRejectedToRejectSubmissionConverter();

        final RejectSubmission commandPayload = converter.convert(payload);

        final Metadata metadata = metadataFrom(prosecutionRejected.metadata())
                .withName(REJECT_SUBMISSION_COMMAND)
                .build();
        final Envelope<RejectSubmission> envelope = envelopeFrom(metadata, commandPayload);

        sender.send(envelope);
    }

}
