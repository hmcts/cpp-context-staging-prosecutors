package json;

import uk.gov.moj.cpp.staging.prosecutors.domain.schema.JsonSchemaChecker;
import uk.gov.moj.cpp.staging.prosecutors.domain.schema.ReferencedSchema;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AddressSchemaTest {

    private static JsonSchemaChecker schemaChecker;

    @BeforeAll
    public static void beforeClass() throws IOException {

        schemaChecker = JsonSchemaChecker.forSchema(
                ReferencedSchema.loadSchema("json/address.json")
                        .withoutReferences(),
                "json-examples/address-example.json");
    }

    @Test
    public void testAddress() {
        schemaChecker.checkProperty("address1")
                .allowsValues("200 Street", "ARBITRARY ADDRESS")
                .isMandatory()
                .rejectsEmptyAndNull();
        schemaChecker.checkProperty("address2")
                .allowsValues("200 Street", "ARBITRARY ADDRESS");
        schemaChecker.checkProperty("address3")
                .allowsValues("200 Street", "ARBITRARY ADDRESS");
        schemaChecker.checkProperty("address4")
                .allowsValues("200 Street", "ARBITRARY ADDRESS");
        schemaChecker.checkProperty("address5")
                .allowsValues("200 Street", "ARBITRARY ADDRESS");

    }

}
