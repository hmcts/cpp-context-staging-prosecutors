package uk.gov.moj.cpp.staging.prosecutors.domain.schema;

import java.io.IOException;
import java.io.InputStream;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ReferencedSchema {

    private JSONObject rawSchema;

    public ReferencedSchema(final String schemaPath) throws IOException {

        try (InputStream schemaInputStream = getClass().getClassLoader().getResourceAsStream(schemaPath)) {
            rawSchema = new JSONObject(new JSONTokener(schemaInputStream));
        }
    }

    public static ReferencedSchema loadSchema(final String schemaPath) throws IOException {

        return new ReferencedSchema(schemaPath);
    }

    public Schema getSchema() {
        return SchemaLoader.load(rawSchema);
    }

    public ReferencedSchema withoutReferences() {
        JSONObject properties = rawSchema.getJSONObject("properties");
        removeReferences(properties);
        return this;
    }

    public void removeReferences(final JSONObject jsonObject) {
        for(final String key: jsonObject.keySet()){
            Object object = jsonObject.get(key);
            if(object instanceof JSONObject) {
                JSONObject child = (JSONObject)object;
                if (child.has("$ref")) {
                    jsonObject.getJSONObject(key).remove("$ref");
                }
                removeReferences(child);
            }
        }
    }

    public ReferencedSchema convertToSchema() {
        JSONObject definitions = rawSchema.getJSONObject("definitions");
        rawSchema.remove("properties");
        rawSchema.put("properties", definitions);
        return this;
    }
}

