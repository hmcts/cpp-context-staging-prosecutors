package json;

import uk.gov.moj.cpp.staging.prosecutors.domain.schema.JsonSchemaChecker;
import uk.gov.moj.cpp.staging.prosecutors.domain.schema.ReferencedSchema;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DefendantSchemaTest {

    private static JsonSchemaChecker schemaChecker;

    @BeforeAll
    public static void beforeClass() throws IOException {

        schemaChecker = JsonSchemaChecker.forSchema(
                ReferencedSchema.loadSchema("json/sjp-defendant.json")
                        .withoutReferences(),
                "json-examples/defendant-test-data.json");
    }

    @Test
    public void testProsecutorDefendantId() {

        schemaChecker.checkProperty("prosecutorDefendantId")
                .isMandatoryForSubSchema()
                .allowsValues("ABC-001", "123456789012345678901234567890123456", "a-zA-z-09")
                .rejectsValues("1234567890123456789012345678901234567", "±!@£$%^&*()+_\",<>?/~");
    }

    @Test
    public void testProsecutorDefendantCosts() {

        schemaChecker.checkProperty("prosecutorCosts")
                .isMandatoryForSubSchema()
                .allowsValues("12.03", "12", "12.1", "0.12");

    }

}
