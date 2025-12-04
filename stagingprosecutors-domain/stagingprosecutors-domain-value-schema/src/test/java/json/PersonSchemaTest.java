package json;

import uk.gov.moj.cpp.staging.prosecutors.domain.schema.JsonSchemaChecker;
import uk.gov.moj.cpp.staging.prosecutors.domain.schema.ReferencedSchema;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PersonSchemaTest {

    private static JsonSchemaChecker schemaChecker;

    @BeforeAll
    public static void beforeClass() throws IOException {

        schemaChecker = JsonSchemaChecker.forSchema(
                ReferencedSchema.loadSchema("json/sjp-person.json")
                        .withoutReferences(),
                "json-examples/person-example.json");
    }

    @Test
    public void testTitle() {
        schemaChecker.checkProperty("title");
    }

    @Test
    public void testForename() {

        schemaChecker.checkProperty("forename")
                .isMandatory()
                .allowsValues("John", "Jean-Paul")
                .rejectsValues("Name  with double space")
                .rejectsEmptyAndNull();
    }

    @Test
    public void testSurname() {

        schemaChecker.checkProperty("surname")
                .isMandatory()
                .allowsValues("Smith", "Brooke-Taylor")
                .rejectsValues("Name  with double space")
                .rejectsEmptyAndNull();
    }

    @Test
    public void testAddress() {

        schemaChecker.checkProperty("address")
                .isMandatory();
    }

    @Test
    public void testSelfDefinedInformation() {

        schemaChecker.checkProperty("selfDefinedInformation")
                .isMandatory();
    }

    @Test
    public void testNationalInsuranceNumber() {

        schemaChecker.checkProperty("nationalInsuranceNumber")
                .allowsValues("AA123456C")
                .rejectsValues("DB123456C", "GB123456C");
    }

}
