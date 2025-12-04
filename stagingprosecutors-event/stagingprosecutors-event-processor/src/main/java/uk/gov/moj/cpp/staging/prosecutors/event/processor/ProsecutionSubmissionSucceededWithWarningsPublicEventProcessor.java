package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSubmissionSuccessfulWithWarnings;
import uk.gov.moj.cps.stagingprosecutors.domain.event.ProsecutionSubmissionSucceededWithWarnings;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionSubmissionSucceededWithWarningsPublicEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionSubmissionSucceededWithWarningsPublicEventProcessor.class);

    @Inject
    private Sender sender;

    @Handles("public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings")
    public void prosecutionSubmissionSucceededWithWarnings(final Envelope<ProsecutionSubmissionSucceededWithWarnings> prosecutionSubmissionSucceededWithWarningsEnvelope) {

        final MetadataBuilder metadataBuilder = metadataFrom(prosecutionSubmissionSucceededWithWarningsEnvelope.metadata())
                .withName("stagingprosecutors.command.receive-submission-successful-with-warnings");

        final ProsecutionSubmissionSucceededWithWarnings payload = prosecutionSubmissionSucceededWithWarningsEnvelope.payload();
        final UUID externalId = payload.getExternalId();

        if (externalId != null && CPPI.equals(payload.getChannel())) {
            final ReceiveSubmissionSuccessfulWithWarnings receiveSubmissionSuccessfulWithWarnings =
                    ReceiveSubmissionSuccessfulWithWarnings.receiveSubmissionSuccessfulWithWarnings()
                            .withSubmissionId(externalId)
                            .withWarnings(payload.getWarnings())
                            .withDefendantWarnings(payload.getDefendantWarnings())
                            .build();
            sender.send(
                    envelopeFrom(
                            metadataBuilder,
                            receiveSubmissionSuccessfulWithWarnings
                    ));
        } else {
            LOGGER.info("Message unrelated to CPPI channel for rejecting case.  Not processing");
        }
    }
}
