package uk.gov.moj.cpp.staging.prosecutorapi.query.api.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResultsV1ResponseTransformerTest {

    private ResultsV1ResponseTransformer transformer;

    @BeforeEach
    public void setUp() {
        transformer = new ResultsV1ResponseTransformer();
    }

    @Test
    public void shouldExtractVerdictCodeAsTopLevelFieldOnOffenceAndRemoveVerdictObject() {
        final JsonObject response = buildResponse(
                offenceWith(buildVerdict("G", "2020-03-12", "FOUND_GUILTY")));

        final JsonObject transformed = transformer.transform(response);

        final JsonObject offence = firstOffence(transformed);
        assertThat(offence.getString("verdictCode"), is("G"));
        assertThat(offence.get("verdict"), is(nullValue()));
    }

    @Test
    public void shouldPreserveVerdictCodeWhenVerdictHasAllThreeFields() {
        final JsonObject response = buildResponse(
                offenceWith(buildVerdict("N", "2021-05-10", "FOUND_NOT_GUILTY")));

        final JsonObject transformed = transformer.transform(response);

        assertThat(firstOffence(transformed).getString("verdictCode"), is("N"));
    }

    @Test
    public void shouldLeaveOffenceUnchangedWhenNoVerdictPresent() {
        final JsonObject offenceNoVerdict = createObjectBuilder()
                .add("offenceCode", "PS90010")
                .add("orderIndex", 1)
                .add("offenceTitle", "Some offence")
                .build();
        final JsonObject response = buildResponse(offenceNoVerdict);

        final JsonObject transformed = transformer.transform(response);

        final JsonObject offence = firstOffence(transformed);
        assertThat(offence.get("verdict"), is(nullValue()));
        assertThat(offence.get("verdictCode"), is(nullValue()));
        assertThat(offence.getString("offenceCode"), is("PS90010"));
    }

    @Test
    public void shouldPreserveAllOtherOffenceFields() {
        final JsonObject response = buildResponse(
                offenceWith(buildVerdict("G", "2020-03-12", "FOUND_GUILTY")));

        final JsonObject offence = firstOffence(transformer.transform(response));

        assertThat(offence.getString("offenceCode"), is("PS90010"));
        assertThat(offence.getInt("orderIndex"), is(1));
        assertThat(offence.getString("offenceTitle"), is("Some offence"));
        assertThat(offence.getString("pleaValue"), is("NOT_GUILTY"));
    }

    @Test
    public void shouldPreserveAllNonOffenceFieldsInResponse() {
        final JsonObject response = buildResponse(
                offenceWith(buildVerdict("G", "2020-03-12", "FOUND_GUILTY")));

        final JsonObject transformed = transformer.transform(response);

        assertThat(transformed.getString("prosecutionAuthorityCode"), is("TFL"));
        assertThat(transformed.getString("prosecutionAuthorityOuCode"), is("GTL0001"));
    }

    private JsonObject buildVerdict(final String verdictCode, final String verdictDate, final String verdictType) {
        return createObjectBuilder()
                .add("verdictCode", verdictCode)
                .add("verdictDate", verdictDate)
                .add("verdictType", verdictType)
                .build();
    }

    private JsonObject offenceWith(final JsonObject verdict) {
        return createObjectBuilder()
                .add("offenceCode", "PS90010")
                .add("orderIndex", 1)
                .add("offenceTitle", "Some offence")
                .add("pleaValue", "NOT_GUILTY")
                .add("verdict", verdict)
                .build();
    }

    private JsonObject buildResponse(final JsonObject offence) {
        return createObjectBuilder()
                .add("prosecutionAuthorityCode", "TFL")
                .add("prosecutionAuthorityOuCode", "GTL0001")
                .add("hearingVenue", createObjectBuilder()
                        .add("courtHouse", "Test Court")
                        .add("courtSessions", javax.json.Json.createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("courtRoom", "Room 1")
                                        .add("hearingStartTime", "2020-03-12")
                                        .add("defendants", javax.json.Json.createArrayBuilder()
                                                .add(createObjectBuilder()
                                                        .add("name", "Fred Smith")
                                                        .add("address1", "Flat 1")
                                                        .add("prosecutionCasesOrApplications", javax.json.Json.createArrayBuilder()
                                                                .add(createObjectBuilder()
                                                                        .add("caseOrApplicationReference", "TFL123")
                                                                        .add("offences", javax.json.Json.createArrayBuilder()
                                                                                .add(offence)))))))))
                .build();
    }

    private JsonObject firstOffence(final JsonObject response) {
        return response
                .getJsonObject("hearingVenue")
                .getJsonArray("courtSessions").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("prosecutionCasesOrApplications").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0);
    }
}
