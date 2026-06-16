package uk.gov.moj.cpp.staging.prosecutorapi.query.api.converter;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

@ApplicationScoped
public class ResultsV1ResponseTransformer {

    private static final String VERDICT = "verdict";

    public JsonObject transform(final JsonObject payload) {
        return deepTransformObject(payload);
    }

    private static JsonObject deepTransformObject(final JsonObject object) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        object.entrySet().stream()
                .filter(e -> !VERDICT.equals(e.getKey()))
                .forEach(e -> {
                    if (e.getValue().getValueType() == JsonValue.ValueType.OBJECT) {
                        builder.add(e.getKey(), deepTransformObject(e.getValue().asJsonObject()));
                    } else if (e.getValue().getValueType() == JsonValue.ValueType.ARRAY) {
                        builder.add(e.getKey(), deepTransformArray(e.getValue().asJsonArray()));
                    } else {
                        builder.add(e.getKey(), e.getValue());
                    }
                });
        if (object.containsKey(VERDICT)) {
            final JsonObject verdict = object.getJsonObject(VERDICT);
            if (verdict.containsKey("verdictCode")) {
                builder.add("verdictCode", verdict.getString("verdictCode"));
            }
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
