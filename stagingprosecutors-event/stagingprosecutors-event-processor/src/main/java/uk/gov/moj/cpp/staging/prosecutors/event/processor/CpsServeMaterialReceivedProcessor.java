package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.converter.CpsServeBcmReceivedToCpsBcmReceivedDetailsConverter;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.converter.CpsServeCotrReceivedToCpsCotrReceivedDetailsConverter;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.converter.CpsServePetReceivedToCpsPetReceivedDetailsConverter;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.converter.CpsServePtphReceivedToCpsPtphReceivedDetailsConverter;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.converter.CpsUpdateCotrReceivedToCpsUpdateCotrReceivedDetailsConverter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeBcmReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePtphReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsUpdateCotrReceived;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CpsServeMaterialReceivedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CpsServeMaterialReceivedProcessor.class);
    @Inject
    private Sender sender;

    @Handles("stagingprosecutors.event.cps-serve-pet-received")
    public void onCpsServePetReceived(final Envelope<CpsServePetReceived> cpsServePetReceivedEnvelope) {

        LOGGER.info("stagingprosecutors.event.cps-serve-pet-received");
        final CpsServePetReceived cpsServePetReceived = cpsServePetReceivedEnvelope.payload();

        final MetadataBuilder builder = metadataFrom(cpsServePetReceivedEnvelope.metadata());
        final Metadata metadata = builder.withName("public.stagingprosecutors.cps-serve-pet-received").build();

        sender.send(envelopeFrom(metadata, new CpsServePetReceivedToCpsPetReceivedDetailsConverter().convert(cpsServePetReceived)));

        LOGGER.info("Raised the public event public.stagingprosecutors.cps-pet-received");
    }

    @Handles("stagingprosecutors.event.cps-serve-bcm-received")
    public void onCpsServeBcmReceived(final Envelope<CpsServeBcmReceived> cpsServeBcmReceivedEnvelope) {

        LOGGER.info("stagingprosecutors.event.cps-serve-bcm-received");
        final CpsServeBcmReceived cpsServeBcmReceived = cpsServeBcmReceivedEnvelope.payload();

        final MetadataBuilder builder = metadataFrom(cpsServeBcmReceivedEnvelope.metadata());
        final Metadata metadata = builder.withName("public.stagingprosecutors.cps-serve-bcm-received").build();

        sender.send(envelopeFrom(metadata, new CpsServeBcmReceivedToCpsBcmReceivedDetailsConverter().convert(cpsServeBcmReceived)));

        LOGGER.info("Raised the public event public.stagingprosecutors.cps-bcm-received");
    }

    @Handles("stagingprosecutors.event.cps-serve-ptph-received")
    public void onCpsServePtphReceived(final Envelope<CpsServePtphReceived> cpsServePtphReceivedEnvelope) {

        LOGGER.info("stagingprosecutors.event.cps-serve-ptph-received");
        final CpsServePtphReceived cpsServePtphReceived = cpsServePtphReceivedEnvelope.payload();

        final MetadataBuilder builder = metadataFrom(cpsServePtphReceivedEnvelope.metadata());
        final Metadata metadata = builder.withName("public.stagingprosecutors.cps-serve-ptph-received").build();

        sender.send(envelopeFrom(metadata, new CpsServePtphReceivedToCpsPtphReceivedDetailsConverter().convert(cpsServePtphReceived)));

        LOGGER.info("Raised the public event public.stagingprosecutors.cps-ptph-received");
    }

    @Handles("stagingprosecutors.event.cps-serve-cotr-received")
    public void onCpsServeCotrReceived(final Envelope<CpsServeCotrReceived> cpsServeCotrReceivedEnvelope) {

        LOGGER.info("stagingprosecutors.event.cps-serve-cotr-received");
        final CpsServeCotrReceived cpsServeCotrReceived = cpsServeCotrReceivedEnvelope.payload();

        final MetadataBuilder builder = metadataFrom(cpsServeCotrReceivedEnvelope.metadata());
        final Metadata metadata = builder.withName("public.stagingprosecutors.cps-serve-cotr-received").build();

        sender.send(envelopeFrom(metadata, new CpsServeCotrReceivedToCpsCotrReceivedDetailsConverter().convert(cpsServeCotrReceived)));

        LOGGER.info("Raised the public event public.stagingprosecutors.cps-cotr-received");
    }

    @Handles("stagingprosecutors.event.cps-update-cotr-received")
    public void onCpsUpdateCotrReceived(final Envelope<CpsUpdateCotrReceived> cpsUpdateCotrReceivedEnvelope) {
        LOGGER.info("stagingprosecutors.event.cps-update-cotr-received");

        final CpsUpdateCotrReceived cpsUpdateCotrReceived = cpsUpdateCotrReceivedEnvelope.payload();

        final MetadataBuilder builder = metadataFrom(cpsUpdateCotrReceivedEnvelope.metadata());
        final Metadata metadata = builder.withName("public.stagingprosecutors.cps-update-cotr-received").build();

        sender.send(envelopeFrom(metadata, new CpsUpdateCotrReceivedToCpsUpdateCotrReceivedDetailsConverter().convert(cpsUpdateCotrReceived)));

        LOGGER.info("Raised the public event public.stagingprosecutors.cps-update-cotr-received");
    }
}
