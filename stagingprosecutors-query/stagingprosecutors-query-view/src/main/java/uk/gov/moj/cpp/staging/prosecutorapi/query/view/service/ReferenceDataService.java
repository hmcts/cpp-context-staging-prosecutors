package uk.gov.moj.cpp.staging.prosecutorapi.query.view.service;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class ReferenceDataService {

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    private static final String REFERENCE_DATA_QUERY_PROSECUTOR_BY_OUCODE = "referencedata.query.get.prosecutor.by.oucode";

    public Optional<JsonObject> getProsecutorByOuCode(final String ouCode) {

        final JsonEnvelope request = JsonEnvelope.envelopeFrom(metadataBuilder()
                        .withId(randomUUID())
                        .withName(REFERENCE_DATA_QUERY_PROSECUTOR_BY_OUCODE),
                createObjectBuilder()
                        .add("oucode", ouCode)
                        .build()
        );
        final Envelope<JsonObject> responseEnvelope = requester.requestAsAdmin(request, JsonObject.class);

        if (JsonValue.NULL.equals(responseEnvelope.payload())) {
            return empty();
        }
        return of(responseEnvelope.payload());
    }
}

