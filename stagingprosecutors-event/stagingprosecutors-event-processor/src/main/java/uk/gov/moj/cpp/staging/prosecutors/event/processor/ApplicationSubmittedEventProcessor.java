package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ApplicationSubmitted;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class ApplicationSubmittedEventProcessor {

    @Inject
    private Sender sender;

    @Handles("stagingprosecutors.event.application-submitted")
    public void applicationSubmitted(final Envelope<ApplicationSubmitted> applicationSubmittedEnvelope) {

        final Metadata applicationSubmittedCommandMetadata = metadataFrom(applicationSubmittedEnvelope.metadata())
                .withName("prosecutioncasefile.command.submit-application")
                .build();

        final Envelope<ApplicationSubmitted> envelope = envelopeFrom(applicationSubmittedCommandMetadata, applicationSubmittedEnvelope.payload());

        sender.sendAsAdmin(envelope);
    }
}
