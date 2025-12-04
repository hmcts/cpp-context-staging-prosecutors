package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static com.jayway.jsonassert.JsonAssert.with;
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
import uk.gov.moj.cpp.staging.prosecutorapi.utils.commandclient.CommandExecutor;

import java.io.StringReader;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SchemaValidationMandatoryIT {

    @BeforeEach
    public void setup() {
        new WiremockUtils()
                .stubPost("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/initiate-sjp-prosecution")
                .stubIdMapperRecordingNewAssociation();
    }

    @Test
    public void mandatoryHappy() {
        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .defendant(getHappyDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
    }

    @Test
    public void mandatoryHappyV2() {
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(OUCODE)
                .build();

        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getHappyDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
    }

    private Defendant getHappyDefendant() {
        final Person person = Person.builder()
                .nationalInsuranceNumber(null)
                .build();

        return Defendant.builder()
                .defendantPerson(person)
                .organisation(null)
                .build();
    }

    @Test
    public void mandatoryNull() {
        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .prosecutionSubmissionDetails(getProsecutionSubmissionDetailsWithNullValues())
                .defendant(getDefendantWithNullValues())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        submitSjpProsecutionResponse.ifPresent(this::assertMandatoryNullValues);
    }

    @Test
    public void mandatoryNullV2() {
        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(getProsecutionSubmissionDetailsWithNullValues())
                .defendant(getDefendantWithNullValues())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        submitSjpProsecutionResponse.ifPresent(this::assertMandatoryNullValues);
    }

    private ProsecutionSubmissionDetails getProsecutionSubmissionDetailsWithNullValues() {
        return ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(null)
                .build();
    }

    private Defendant getDefendantWithNullValues() {
        final Address address = Address.builder()
                .address1(null)
                .postcode(null)
                .build();

        final Person person = Person.builder()
                .title(null)
                .forename(null)
                .surname(null)
                .nationalInsuranceNumber(null)
                .address(address)
                .selfDefinedInformation(null)
                .build();

        return Defendant.builder()
                .hearingLanguage(null)
                .documentationLanguage(null)
                .offences(new Offence[]{null})
                .defendantPerson(person)
                .build();
    }

    private void assertMandatoryNullValues(final Response submitSjpProsecutionResponse) {
        assertThat(submitSjpProsecutionResponse.getStatus(), equalTo(400));

        final JsonObject responseJson = responseToJsonObject(submitSjpProsecutionResponse.readEntity(String.class));
        final JsonValue validationErrors = responseJson.get("validationErrors");
        final String validationTrace = validationErrors.toString();

        with(validationTrace)
                .assertEquals("$.message", "#: 11 schema violations found");
        assertThat(validationTrace, containsString("#/prosecutionSubmissionDetails:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/address:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson:"));
        assertThat(validationTrace, containsString("#/defendant/offences/0:"));
        assertThat(validationTrace, containsString("#/defendant:"));
    }

    @Test
    public void mandatoryMissing() {
        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .prosecutionSubmissionDetails(getMandatoryMissingProsecutionSubmissionDetails())
                .defendant(getMandatoryMissingDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(400));
        final JsonObject responseJson = responseToJsonObject(submitSjpProsecutionResponse.get().readEntity(String.class));

        final JsonValue validationErrors = responseJson.get("validationErrors");
        final String validationTrace = validationErrors.toString();
        assertMandatoryMissingValues(validationTrace);

    }

    @Test
    public void mandatoryMissingV2() {
        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(getMandatoryMissingProsecutionSubmissionDetails())
                .defendant(getMandatoryMissingDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(400));
        final JsonObject responseJson = responseToJsonObject(submitSjpProsecutionResponse.get().readEntity(String.class));

        final JsonValue validationErrors = responseJson.get("validationErrors");
        final String validationTrace = validationErrors.toString();

        with(validationTrace)
                .assertEquals("$.message", "#: 15 schema violations found");

        assertMandatoryMissingValues(validationTrace);
    }

    private ProsecutionSubmissionDetails getMandatoryMissingProsecutionSubmissionDetails() {
        return ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority("")
                .writtenChargePostingDate("")
                .urn("")
                .build();
    }

    private Defendant getMandatoryMissingDefendant() {
        SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.builder()
                .ethnicity("")
                .build();

        Address address = Address.builder()
                .address1("")
                .postcode("")
                .build();

        Person person = Person.builder()
                .title("")
                .forename("")
                .surname("")
                .nationalInsuranceNumber("")
                .address(address)
                .selfDefinedInformation(selfDefinedInformation)
                .build();

        return Defendant.builder()
                .hearingLanguage("")
                .documentationLanguage("")
                .defendantPerson(person)
                .build();
    }

    private void assertMandatoryMissingValues(final String validationTrace) {
        assertThat(validationTrace, containsString("#/prosecutionSubmissionDetails/prosecutingAuthority:"));
        assertThat(validationTrace, containsString("#/prosecutionSubmissionDetails/urn:"));
        assertThat(validationTrace, containsString("#/defendant/documentationLanguage:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/address/postcode:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/address/address1:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/forename:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/surname:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/nationalInsuranceNumber:"));
        assertThat(validationTrace, containsString("#/defendant/hearingLanguage:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/selfDefinedInformation/ethnicity:"));
    }

    @Test
    public void mandatoryEmpty() {
        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .prosecutionSubmissionDetails(getMandatoryEmptyProsecutionSubmissionDetails())
                .defendant(getMandatoryEmptyDefendant())
                .build();

        final CommandExecutor executor = sjpProsecution.getExecutor();
        executor.getMapper().setSerializationInclusion(JsonInclude.Include.ALWAYS);
        final Optional<Response> submitSjpProsecutionResponse = executor.executeSync();
        submitSjpProsecutionResponse.ifPresent(this::assertMandatoryEmptyValues);
    }

    @Test
    public void mandatoryEmptyV2() {

        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(getMandatoryEmptyProsecutionSubmissionDetails())
                .defendant(getMandatoryEmptyDefendant())
                .build();

        final CommandExecutor executor = sjpProsecutionV2.getExecutor();
        executor.getMapper().setSerializationInclusion(JsonInclude.Include.ALWAYS);
        final Optional<Response> submitSjpProsecutionResponse = executor.executeSync();
        submitSjpProsecutionResponse.ifPresent(this::assertMandatoryEmptyValues);
    }

    private ProsecutionSubmissionDetails getMandatoryEmptyProsecutionSubmissionDetails() {
        return ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(null)
                .urn(null)
                .build();
    }

    private Defendant getMandatoryEmptyDefendant() {
        final Address address = Address.builder()
                .address1(null)
                .postcode(null)
                .build();

        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.builder()
                .ethnicity(null)
                .build();

        final Person person = Person.builder()
                .title(null)
                .forename(null)
                .surname(null)
                .nationalInsuranceNumber(null)
                .address(address)
                .selfDefinedInformation(selfDefinedInformation)
                .build();

        return Defendant.builder()
                .hearingLanguage(null)
                .documentationLanguage(null)
                .defendantPerson(person)
                .offences(null)
                .build();
    }

    private void assertMandatoryEmptyValues(final Response submitSjpProsecutionResponse) {
        assertThat(submitSjpProsecutionResponse.getStatus(), equalTo(400));

        final JsonObject responseJson = responseToJsonObject(submitSjpProsecutionResponse.readEntity(String.class));
        final JsonValue validationErrors = responseJson.get("validationErrors");
        final String validationTrace = validationErrors.toString();

        with(validationTrace)
                .assertEquals("$.message", "#: 17 schema violations found");
        assertThat(validationTrace, containsString("#/prosecutionSubmissionDetails/prosecutingAuthority:"));
        assertThat(validationTrace, containsString("#/prosecutionSubmissionDetails/urn:"));
        assertThat(validationTrace, containsString("#/defendant/documentationLanguage:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/address/postcode:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/address/address1:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/title:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/forename:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/surname:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/nationalInsuranceNumber:"));
        assertThat(validationTrace, containsString("#/defendant/offences:"));
        assertThat(validationTrace, containsString("#/defendant/hearingLanguage:"));
    }

    private JsonObject responseToJsonObject(String response) {
        return Json.createReader(new StringReader(response)).readObject();
    }

}
