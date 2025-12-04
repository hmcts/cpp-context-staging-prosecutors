package json;


import uk.gov.moj.cpp.staging.prosecutors.domain.schema.JsonSchemaChecker;
import uk.gov.moj.cpp.staging.prosecutors.domain.schema.ReferencedSchema;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ProsecutionSubmissionDetailsTest {
    private static JsonSchemaChecker schemaChecker;

    @BeforeAll
    public static void beforeClass() throws IOException {

        schemaChecker = JsonSchemaChecker.forSchema(
                ReferencedSchema.loadSchema("json/sjp-prosecution-submission-details.json")
                        .withoutReferences(),
                "json-examples/prosecution-submission-details-test-data.json");
    }

    @Test
    public void testChargeDate() {

        schemaChecker.checkProperty("writtenChargePostingDate")
                .isMandatory()
                .allowsValues("2018-07-16");
    }


    @Test
    public void testUrn() {

        schemaChecker.checkProperty("urn")
                .isMandatory()
                .allowsValues("ABC-001","123456789012345678901234567890123456","a-zA-z-09");
    }
}
