package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSubmissionSuccessful.receiveSubmissionSuccessful;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSubmissionSuccessful;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceeded;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionSubmissionSucceededPublicEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionSubmissionSucceededPublicEventProcessor.class);

    @Inject
    private Sender sender;

    @Handles("public.prosecutioncasefile.prosecution-submission-succeeded")
    public void prosecutionSubmissionSucceeded(final Envelope<ProsecutionSubmissionSucceeded> prosecutionSubmissionSucceededEnvelope) {

        final MetadataBuilder metadataBuilder = metadataFrom(prosecutionSubmissionSucceededEnvelope.metadata())
                .withName("stagingprosecutors.command.receive-submission-successful");

        final ProsecutionSubmissionSucceeded payload = prosecutionSubmissionSucceededEnvelope.payload();
        final Optional<UUID> externalId = ofNullable(payload.getExternalId());

        if (externalId.isPresent() && CPPI.equals(payload.getChannel())) {

            final ReceiveSubmissionSuccessful receiveSubmissionSuccessful = receiveSubmissionSuccessful()
                    .withSubmissionId(externalId.get())
                    .build();

            sender.send(
                    envelopeFrom(
                            metadataBuilder,
                            receiveSubmissionSuccessful
                    ));
        } else {
            LOGGER.info("Message unrelated to CPPI channel for rejecting case.  Not processing");
        }
    }
}
