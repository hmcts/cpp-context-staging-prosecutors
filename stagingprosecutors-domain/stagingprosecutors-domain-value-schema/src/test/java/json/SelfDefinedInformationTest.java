package json;


import uk.gov.moj.cpp.staging.prosecutors.domain.schema.JsonSchemaChecker;
import uk.gov.moj.cpp.staging.prosecutors.domain.schema.ReferencedSchema;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SelfDefinedInformationTest {

    private static JsonSchemaChecker schemaChecker;

    @BeforeAll
    public static void beforeClass() throws IOException {

        schemaChecker = JsonSchemaChecker.forSchema(
                ReferencedSchema.loadSchema("json/self-defined-information.json"), "json-examples/self-defined-information-test-data.json");
    }

    @Test
    public void testEthnicity() {

        schemaChecker.checkProperty("ethnicity")
                .allowsValues("W1", "W2", "W9", "M1", "M2", "M3", "M9",
                        "A1", "A2", "A3", "A9", "B1", "B2", "B9", "O1", "O9", "NS")
                .rejectsValues("W0", "ZZ")
                .rejectsEmptyAndNull();
    }

    @Test
    public void testNationality() {

        schemaChecker.checkProperty("nationality")
                .allowsValues("ABC", "AB", "A")
                .rejectsValues("ABCD");
    }

    @Test
    public void testAdditionalNationality() {

        schemaChecker.checkProperty("additionalNationality")
                .allowsValues("ABC", "AB", "A")
                .rejectsValues("ABCD");
    }

    @Test
    public void testGender() {

        schemaChecker.checkProperty("gender")
                .isMandatory();
    }
}
