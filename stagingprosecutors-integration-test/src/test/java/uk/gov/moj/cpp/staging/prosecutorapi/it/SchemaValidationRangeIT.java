package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static com.jayway.jsonassert.JsonAssert.with;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.OUCODE;

import uk.gov.moj.cpp.staging.prosecutorapi.model.command.SjpProsecutionSubmissionClient;
import uk.gov.moj.cpp.staging.prosecutorapi.model.command.SjpProsecutionSubmissionClientV2;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Address;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Defendant;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Offence;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Person;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.ProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.SelfDefinedInformation;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;

import java.io.StringReader;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SchemaValidationRangeIT {

    private static final WiremockUtils wiremockUtils = new WiremockUtils();

    @BeforeEach
    public void setup() {
        wiremockUtils.stubIdMapperRecordingNewAssociation();
    }

    @Test
    public void rangeEqualMin() {
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority("GAEAA01")
                .build();

        final  SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getMinRangeDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
    }

    @Test
    public void rangeEqualMinV2() {

        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(OUCODE)
                .build();

        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getMinRangeDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
    }

    private Defendant getMinRangeDefendant() {
        final Address address = Address.builder()
                .address1("1")
                .postcode("M1 1AA")
                .build();

        final Person person = Person.builder()
                .title("Mr")
                .forename("A")
                .surname("A")
                .nationalInsuranceNumber("AB123456D")
                .address(address)
                .build();

        return Defendant.builder()
                .hearingLanguage("E")
                .documentationLanguage("E")
                .defendantPerson(person)
                .organisation(null)
                .build();
    }

    @Test
    public void rangeEqualMax() {

        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority("GAEAA01")
                .build();

        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getMaxRangeDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
    }

    @Test
    public void rangeEqualMaxV2() {
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(OUCODE)
                .build();

        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getMaxRangeDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
    }

    private Defendant getMaxRangeDefendant() {
        final Address address = Address.builder()
                .address1(randomAlphanumeric(35))
                .postcode("BT12 9TS")
                .build();

        final Person person = Person.builder()
                .title("Mr")
                .forename(randomAlphanumeric(35))
                .surname(randomAlphanumeric(35))
                .nationalInsuranceNumber("AB123456D")
                .address(address)
                .build();

        return Defendant.builder()
                .hearingLanguage("E")
                .documentationLanguage("E")
                .defendantPerson(person)
                .organisation(null)
                .build();
    }

    @Test
    public void rangeBelowMin() {
        final String prosecutingAuthority = "GAEAA01";

        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(prosecutingAuthority)
                .build();


        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getRangeBelowMinDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        submitSjpProsecutionResponse.ifPresent(this::assertRangeBelowMinValues);

    }

    @Test
    public void rangeBelowMinV2() {
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(OUCODE)
                .build();

        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getRangeBelowMinDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        submitSjpProsecutionResponse.ifPresent(this::assertRangeBelowMinValues);

    }

    private Defendant getRangeBelowMinDefendant() {
        final String nationalInsuranceNumber = "12345678";

        final Address address = Address.builder()
                .address1("")
                .postcode("A1 1A")
                .build();

        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.builder()
                .ethnicity("")
                .build();

        final Person person = Person.builder()
                .title("Mr")
                .forename("")
                .surname("")
                .nationalInsuranceNumber(nationalInsuranceNumber)
                .address(address)
                .selfDefinedInformation(selfDefinedInformation)
                .build();

        return Defendant.builder()
                .hearingLanguage("E")
                .documentationLanguage("E")
                .defendantPerson(person)
                .organisation(null)
                .offences(null)
                .build();
    }

    private void assertRangeBelowMinValues(final Response submitSjpProsecutionResponse) {
        assertThat(submitSjpProsecutionResponse.getStatus(), equalTo(400));

        final JsonObject responseJson = responseToJsonObject(submitSjpProsecutionResponse.readEntity(String.class));

        String validationTrace = responseJson.get("validationErrors").toString();

        assertThat(validationTrace, containsString("#/defendant/defendantPerson/address/postcode:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/address/address1:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/forename:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/surname:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/nationalInsuranceNumber:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/selfDefinedInformation/ethnicity:"));
        assertThat(validationTrace, containsString("#/defendant: required key [offences] not found"));
    }


    @Test
    public void rangeAboveMax() {
        ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority("123456789")
                .build();

        SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getRangeAboveMaxDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(400));
        final JsonObject responseJson = responseToJsonObject(submitSjpProsecutionResponse.get().readEntity(String.class));
        final String validationTrace = responseJson.get("validationErrors").toString();
        with(validationTrace)
                .assertEquals("$.message", "#: 15 schema violations found");

        assertThat(validationTrace, containsString("#/prosecutionSubmissionDetails/prosecutingAuthority: string [123456789] does not match pattern ^GAEAA01$"));
        assertRangeAboveMaxValues(validationTrace);
    }

    @Test
    public void rangeAboveMaxV2() {
        ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority("123456789")
                .build();

        SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getRangeAboveMaxDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(400));
        final JsonObject responseJson = responseToJsonObject(submitSjpProsecutionResponse.get().readEntity(String.class));
        final String validationTrace = responseJson.get("validationErrors").toString();
        with(validationTrace)
                .assertEquals("$.message", "#: 14 schema violations found");

        assertRangeAboveMaxValues(validationTrace);
    }

    private Defendant getRangeAboveMaxDefendant() {
        Offence offence1 = Offence.builder()
                .offenceDateCode(7)
                .chargeDate("2018-07-111")
                .build();

        Offence offence2 = Offence.builder()
                .offenceDateCode(4)
                .build();

        SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.builder()
                .ethnicity("QWERTY")
                .build();

        Address address = Address.builder()
                .address1(randomAlphanumeric(36))
                .postcode("ZZ9X 9ZZZ")
                .build();

        Person person = Person.builder()
                .title("Mr")
                .forename(randomAlphanumeric(36))
                .surname(randomAlphanumeric(36))
                .nationalInsuranceNumber("1234567890")
                .address(address)
                .selfDefinedInformation(selfDefinedInformation)
                .build();

        return Defendant.builder()
                .hearingLanguage("E")
                .documentationLanguage("E")
                .defendantPerson(person)
                .offences(new Offence[]{offence1, offence2})
                .build();
    }

    private void assertRangeAboveMaxValues(final String validationTrace) {
        assertThat(validationTrace, containsString("#/prosecutionSubmissionDetails/prosecutingAuthority: expected maxLength: 7, actual: 9"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/address/postcode:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/address/address1:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/forename:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/surname:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/nationalInsuranceNumber:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/selfDefinedInformation/ethnicity:"));
        assertThat(validationTrace, containsString("#/defendant/offences/0:"));
        assertThat(validationTrace, containsString("#/defendant/offences/0/offenceDateCode"));
    }

    private JsonObject responseToJsonObject(String response) {
        return Json.createReader(new StringReader(response)).readObject();
    }

}
