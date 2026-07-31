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

class HmctsResultsV2ExampleTest {

    private JsonObject root;

    @BeforeEach
    void loadExample() throws IOException {
        final String exampleJson = Files.readString(Paths.get("src/raml/json/example/hmcts.results.v2.json"));
        root = createReader(new StringReader(exampleJson)).readObject();
    }

    @Test
    void exampleContainsVerdictObjectWithAllThreeFields() {
        final JsonObject verdict = firstOffence().getJsonObject("verdict");

        assertThat(verdict, is(notNullValue()));
        assertThat(verdict.getString("verdictCode"), is("G"));
        assertThat(verdict.getString("verdictDate"), is("2020-03-12"));
        assertThat(verdict.getString("verdictType"), is("FOUND_GUILTY"));
    }

    @Test
    void exampleDoesNotContainFlatVerdictCodeOnOffenceToConfirmFullStructure() {
        assertThat(firstOffence().get("verdictCode"), is(nullValue()));
        assertThat(firstOffence().getJsonObject("verdict"), is(notNullValue()));
    }

    @Test
    void exampleContainsOffenceWithNoVerdictToShowOptionalField() {
        final JsonObject offenceWithoutVerdict = root
                .getJsonArray("hearingVenues").getJsonObject(0)
                .getJsonArray("courtSessions").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(1)
                .getJsonArray("prosecutionCasesOrApplications").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0);

        assertThat(offenceWithoutVerdict.get("verdict"), is(nullValue()));
        assertThat(offenceWithoutVerdict.get("verdictCode"), is(nullValue()));
    }

    private JsonObject firstOffence() {
        return root
                .getJsonArray("hearingVenues").getJsonObject(0)
                .getJsonArray("courtSessions").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("prosecutionCasesOrApplications").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0);
    }
}
