package uk.gov.moj.cpp.staging.prosecutorapi.query.api;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.slf4j.Logger;

@ServiceComponent(QUERY_API)
public class ResultsQueryApi {

    private static final Logger LOGGER = getLogger(ResultsQueryApi.class);
    private static final String GET_RESULTS_API = "results.prosecutor-results";
    private static final String GET_RESULTS = "hmcts.results.v1";

    @Inject
    private Requester requester;

    @Handles("hmcts.results.v1")
    public JsonEnvelope getResults(final JsonEnvelope query) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Results requested: {}", query.toObfuscatedDebugString());
        }
        final JsonEnvelope resultsQueryEnvelope = envelopeFrom(
                metadataFrom(query.metadata())
                        .withName(GET_RESULTS_API)
                        .build(), query.payloadAsJsonObject());

        final JsonEnvelope resultsResponseEnvelope = requester.request(resultsQueryEnvelope);
        return envelopeFrom(
                metadataFrom(resultsResponseEnvelope.metadata())
                        .withName(GET_RESULTS)
                        .build(), flattenVerdictCodeInPayload(resultsResponseEnvelope.payloadAsJsonObject()));
    }

    private JsonObject flattenVerdictCodeInPayload(final JsonObject payload) {
        return deepTransformObject(payload);
    }

    private static JsonObject deepTransformObject(final JsonObject object) {
        final JsonObjectBuilder builder = createObjectBuilder();
        object.entrySet().stream()
                .filter(e -> !"verdict".equals(e.getKey()))
                .forEach(e -> {
                    if (e.getValue().getValueType() == JsonValue.ValueType.OBJECT) {
                        builder.add(e.getKey(), deepTransformObject(e.getValue().asJsonObject()));
                    } else if (e.getValue().getValueType() == JsonValue.ValueType.ARRAY) {
                        builder.add(e.getKey(), deepTransformArray(e.getValue().asJsonArray()));
                    } else {
                        builder.add(e.getKey(), e.getValue());
                    }
                });
        if (object.containsKey("verdict")) {
            builder.add("verdictCode", object.getJsonObject("verdict").getString("verdictCode"));
        }
        return builder.build();
    }

    private static JsonArray deepTransformArray(final JsonArray array) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        array.forEach(item -> {
            if (item.getValueType() == JsonValue.ValueType.OBJECT) {
                builder.add(deepTransformObject(item.asJsonObject()));
            } else if (item.getValueType() == JsonValue.ValueType.ARRAY) {
                builder.add(deepTransformArray(item.asJsonArray()));
            } else {
                builder.add(item);
            }
        });
        return builder.build();
    }
}