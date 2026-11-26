package uk.gov.moj.cpp.staging.prosecutors.event.processor.utils;

import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.METADATA;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;

import uk.gov.justice.services.core.accesscontrol.AccessControlViolationException;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.function.Function;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

//TODO Remove (ATCM-3474)
//Custom metadata fields are lost when envelop is transferred via http, workaround is to pass them in payload.
public class EnvelopeHelper {

    @Inject
    private SystemUserProvider systemUserProvider;

    public JsonEnvelope withMetadataInPayload(final JsonEnvelope envelope) {
        final Function<String, Boolean> excludeCausation = key -> !"causation".equals(key);


        final Metadata metadataWithSystemUser = metadataFrom(envelope.metadata())
                .withUserId(systemUserProvider.getContextSystemUserId().orElseThrow(() -> new AccessControlViolationException("System user not found")).toString())
                .build();

        final JsonObjectBuilder metadataWithoutCausation = createObjectBuilderWithFilter(metadataWithSystemUser.asJsonObject(), excludeCausation);

        final JsonObject payloadWithMetadata = createObjectBuilder(envelope.payloadAsJsonObject()).add(METADATA, metadataWithoutCausation).build();
        return JsonEnvelope.envelopeFrom(envelope.metadata(), payloadWithMetadata);
    }
}
