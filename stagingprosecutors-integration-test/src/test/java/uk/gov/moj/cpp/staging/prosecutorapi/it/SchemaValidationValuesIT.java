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

import java.io.StringReader;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SchemaValidationValuesIT {

    private static final WiremockUtils wiremockUtils = new WiremockUtils();

    @BeforeEach
    public void setup() {
        wiremockUtils.stubIdMapperRecordingNewAssociation();
    }

    @Test
    public void shouldVerifySchemaWithCorrectValuesV1() {
        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .defendant(Defendant.builder()
                        .organisation(null)
                        .build())
                .build();
        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
    }

    @Test
    public void shouldVerifySchemaWithCorrectValuesV2() {
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(OUCODE)
                .build();

        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(Defendant.builder()
                        .organisation(null)
                        .build())
                .build();
        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
    }

    @Test
    public void shouldVerifySchemaWithCorrectValuesV1_2() {
        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .defendant(getHappyDefendant2())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
    }

    @Test
    public void shouldVerifySchemaWithCorrectValuesV2_2() {
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(OUCODE)
                .build();

        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getHappyDefendant2())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
    }

    @Test
    public void shouldVerifySchemaWithCorrectValuesV1_3() {
        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .defendant(getHappyDefendant3())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
    }

    @Test
    public void shouldVerifySchemaWithCorrectValuesV2_3() {
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(OUCODE)
                .build();

        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getHappyDefendant3())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
    }

    @Test
    public void shouldVerifySchemaWithCorrectValuesV1_4() {
        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .defendant(getHappyDefendant4())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
    }

    @Test
    public void shouldVerifySchemaWithCorrectValuesV2_4() {
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(OUCODE)
                .build();

        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getHappyDefendant4())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
    }

    @Test
    public void shouldVerifySchemaWithMissingValues() {
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority("GAEAA01")
                .urn("")
                .build();

        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getSadDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        submitSjpProsecutionResponse.ifPresent(this::assertSadValues);
    }

    @Test
    public void shouldVerifySchemaWithMissingValuesV2() {
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(OUCODE)
                .urn("  ")
                .build();

        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getSadDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        submitSjpProsecutionResponse.ifPresent(this::assertSadValues);
    }

    @Test
    public void shouldVerifyValuesPostCodeNoSpaceSad() {
        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .defendant(getSadPostCodeNoSpaceDefendant())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        submitSjpProsecutionResponse.ifPresent(this::assertPostCodeNoSpaceSadValues);
    }

    private Defendant getHappyDefendant2() {
        final Offence offence = Offence.builder()
                .offenceDateCode(2)
                .build();

        final Address address = Address.builder()
                .postcode("M60 1NW")
                .build();

        final Person person = Person.builder()
                .title("Mrs")
                .forename("John2")
                .nationalInsuranceNumber("AB123456B")
                .address(address)
                .build();

        return Defendant.builder()
                .defendantPerson(person)
                .organisation(null)
                .offences(new Offence[]{offence})
                .build();
    }

    private Defendant getHappyDefendant3() {
        final Offence offence = Offence.builder()
                .offenceDateCode(3)
                .build();

        final Address address = Address.builder()
                .address1("3 The Street")
                .postcode("CR2 6XH")
                .build();

        final Person person = Person.builder()
                .title("Ms")
                .forename("John Joe")
                .nationalInsuranceNumber("AB123456C")
                .address(address)
                .build();

        return Defendant.builder()
                .hearingLanguage("W")
                .documentationLanguage("W")
                .defendantPerson(person)
                .organisation(null)
                .offences(new Offence[]{offence})
                .build();
    }

    private Defendant getHappyDefendant4() {
        final Offence offence = Offence.builder()
                .offenceDateCode(4)
                .build();

        final Address address = Address.builder()
                .address1("5 The Street")
                .postcode("EC1A 1BB")
                .build();

        final Person person = Person.builder()
                .title("Miss")
                .forename("John")
                .nationalInsuranceNumber("AB123456D")
                .address(address)
                .build();

        return Defendant.builder()
                .hearingLanguage("W")
                .documentationLanguage("W")
                .defendantPerson(person)
                .organisation(null)
                .offences(new Offence[]{offence})
                .build();
    }

    private Defendant getSadDefendant() {
        final Address address = Address.builder()
                .address1("5 The Street")
                .postcode("QC1A 1BB ")
                .build();

        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.builder()
                .ethnicity("W11")
                .build();

        final Person person = Person.builder()
                .title("Sir")
                .forename("paul  macca")
                .surname("mc  cartney")
                .nationalInsuranceNumber("DB123456A")
                .address(address)
                .selfDefinedInformation(selfDefinedInformation)
                .build();

        return Defendant.builder()
                .hearingLanguage("Z")
                .documentationLanguage("Z")
                .defendantPerson(person)
                .organisation(null)
                .build();
    }

    private void assertSadValues(final Response submitSjpProsecutionResponse) {
        assertThat(submitSjpProsecutionResponse.getStatus(), equalTo(400));

        final JsonObject responseJson = responseToJsonObject(submitSjpProsecutionResponse.readEntity(String.class));
        final JsonValue validationErrors = responseJson.get("validationErrors");
        String validationTrace = validationErrors.toString();

        with(validationTrace)
                .assertEquals("$.message", "#: 9 schema violations found");

        assertThat(validationTrace, containsString("#/prosecutionSubmissionDetails/urn:"));
        assertThat(validationTrace, containsString("#/defendant/documentationLanguage:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/address/postcode:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/forename:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/surname:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/nationalInsuranceNumber:"));
        assertThat(validationTrace, containsString("#/defendant/defendantPerson/selfDefinedInformation/ethnicity:"));
        assertThat(validationTrace, containsString("#/defendant/hearingLanguage:"));
    }

    private void assertPostCodeNoSpaceSadValues(final Response submitSjpProsecutionResponse) {
        assertThat(submitSjpProsecutionResponse.getStatus(), equalTo(400));

        final JsonObject responseJson = responseToJsonObject(submitSjpProsecutionResponse.readEntity(String.class));
        final JsonValue validationErrors = responseJson.get("validationErrors");
        String validationTrace = validationErrors.toString();

        assertThat(validationTrace, containsString("#/defendant/defendantPerson/address/postcode: string [EC1A1BB] does not match pattern ^(([gG][iI][rR] {0,}0[aA]{2})|(([aA][sS][cC][nN]|[sS][tT][hH][lL]|[tT][dD][cC][uU]|[bB][bB][nN][dD]|[bB][iI][qQ][qQ]|[fF][iI][qQ][qQ]|[pP][cC][rR][nN]|[sS][iI][qQ][qQ]|[iT][kK][cC][aA]) {0,}1[zZ]{2})|((([a-pr-uwyzA-PR-UWYZ][a-hk-yxA-HK-XY]?[0-9][0-9]?)|(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9][abehmnprv-yABEHMNPRV-Y]))) [0-9][abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))$"));
    }

    private Defendant getSadPostCodeNoSpaceDefendant() {
        final Offence offence = Offence.builder()
                .offenceDateCode(4)
                .build();

        final Address address = Address.builder()
                .address1("5 The Street")
                .postcode("EC1A1BB")
                .build();

        final Person person = Person.builder()
                .title("Miss")
                .forename("John")
                .nationalInsuranceNumber("AB123456D")
                .address(address)
                .build();

        return Defendant.builder()
                .hearingLanguage("W")
                .documentationLanguage("W")
                .defendantPerson(person)
                .organisation(null)
                .offences(new Offence[]{offence})
                .build();
    }

    private JsonObject responseToJsonObject(String response) {
        return Json.createReader(new StringReader(response)).readObject();
    }
}
