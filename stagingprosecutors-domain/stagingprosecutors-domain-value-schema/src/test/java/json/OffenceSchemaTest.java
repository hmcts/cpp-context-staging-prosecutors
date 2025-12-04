package json;

import uk.gov.moj.cpp.staging.prosecutors.domain.schema.JsonSchemaChecker;
import uk.gov.moj.cpp.staging.prosecutors.domain.schema.ReferencedSchema;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OffenceSchemaTest {

    private static JsonSchemaChecker schemaChecker;

    @BeforeAll
    public static void beforeClass() throws IOException {

        schemaChecker = JsonSchemaChecker.forSchema(
                ReferencedSchema.loadSchema("json/sjp-offence.json")
                        .withoutReferences(),
                "json-examples/offence-example.json");
    }

    @Test
    public void testCjsOffenceCode() {

        schemaChecker.checkProperty("cjsOffenceCode")
                .isMandatory()
                .allowsValues("12345678")
                .rejectsValues("123456789");
    }

    @Test
    public void testOffenceCommittedDate() {
        schemaChecker.checkProperty("offenceCommittedDate")
                .isMandatory()
                .allowsValues("2018-07-16");
    }
}