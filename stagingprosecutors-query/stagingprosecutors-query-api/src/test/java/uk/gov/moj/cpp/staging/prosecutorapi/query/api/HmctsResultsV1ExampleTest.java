package uk.gov.moj.cpp.staging.prosecutorapi.query.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HmctsResultsV1ExampleTest {

    private JsonObject root;

    @BeforeEach
    void loadExample() throws IOException {
        final String exampleJson = Files.readString(Paths.get("src/raml/json/example/hmcts.results.v1.json"));
        root = createReader(new StringReader(exampleJson)).readObject();
    }

    @Test
    void exampleContainsFlatVerdictCodeOnOffence() {
        final JsonObject offence = firstOffence();

        assertThat(offence.get("verdictCode"), is(notNullValue()));
        assertThat(offence.getString("verdictCode"), is("G"));
        assertThat(offence.get("verdict"), is(nullValue()));
    }

    @Test
    void exampleDoesNotContainVerdictObjectOnOffenceToConfirmFlatStructure() {
        assertThat(firstOffence().get("verdict"), is(nullValue()));
    }

    @Test
    void exampleContainsOffenceWithNoVerdictToShowOptionalField() {
        final JsonObject offenceWithoutVerdict = root
                .getJsonObject("hearingVenue")
                .getJsonArray("courtSessions").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(1)
                .getJsonArray("prosecutionCasesOrApplications").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0);

        assertThat(offenceWithoutVerdict.get("verdict"), is(nullValue()));
        assertThat(offenceWithoutVerdict.get("verdictCode"), is(nullValue()));
    }

    private JsonObject firstOffence() {
        return root
                .getJsonObject("hearingVenue")
                .getJsonArray("courtSessions").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("prosecutionCasesOrApplications").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0);
    }
}
