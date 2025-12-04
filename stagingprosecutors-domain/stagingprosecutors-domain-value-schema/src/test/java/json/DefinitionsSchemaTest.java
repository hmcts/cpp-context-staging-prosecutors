package json;

import uk.gov.moj.cpp.staging.prosecutors.domain.schema.JsonSchemaChecker;
import uk.gov.moj.cpp.staging.prosecutors.domain.schema.ReferencedSchema;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DefinitionsSchemaTest {

    private static JsonSchemaChecker schemaChecker;

    @BeforeAll
    public static void beforeClass() throws IOException {

        schemaChecker = JsonSchemaChecker.forSchema(
                ReferencedSchema.loadSchema("json/definitions.json")
                        .convertToSchema(),
                "json-examples/definitions-example.json");
    }

    @Test
    public void testNationalInsuranceNumber() {

        schemaChecker.checkProperty("nationalInsuranceNumber")
                .allowsValues("AA223456C")
                .rejectsValues("X")
                .rejectsValues("DB123456C")
                .rejectsValues("GB123456C");
    }

    @Test
    public void testMoney() {

        schemaChecker.checkProperty("money")
                // Allows
                .allowsValues("+123456789012.12")               // Positive 12 digits with 2 decimal places
                .allowsValues("123456789012.12")                // Unsigned 12 digits with 2 decimal places
                .allowsValues("0.00")                           // Unsigned Zero
                .allowsValues("+0.00")                          // Positive Zero
                // Rejects
                .rejectsValues("+1234567890123.12")             // Signed 13 digits with 2 decimal places
                .rejectsValues("1234567890123.12")              // Unsigned 13 digits with 2 decimal places
                .rejectsValues("-10.00", "-0.00")               // Negative signed numbers
                .rejectsValues(".10")                           // Missing integer amounts
                .rejectsValues("0", "10")                       // Numbers with 0 decimal places
                .rejectsValues("0.0", "10.0", "10.1")           // Numbers with 1 decimal place
                .rejectsValues("0.000", "10.000", "10.123");    // Numbers with 3 decimal places
    }
}
