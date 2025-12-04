package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.BusinessValidationsTextResources.FIELD_HOME_NUMBER;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.BusinessValidationsTextResources.FIELD_MOBILE_NUMBER;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.BusinessValidationsTextResources.FIELD_WORK_NUMBER;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.BusinessValidationsTextResources.GENDER_NOT_ACCEPTABLE;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.OUCODE;

import uk.gov.moj.cpp.staging.prosecutorapi.model.command.SjpProsecutionSubmissionClient;
import uk.gov.moj.cpp.staging.prosecutorapi.model.command.SjpProsecutionSubmissionClientV2;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Address;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Defendant;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Offence;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Person;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.ProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.SelfDefinedInformation;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.BusinessValidationsTextResources;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;

import java.io.StringReader;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BusinessRulesValidationMandatoryIT {

    @BeforeEach
    public void setup() {
        new WiremockUtils();
    }

    @Test
    public void businessValidationsShouldFail() {
        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .defendant(getDefendantWithFailedBusinessValidationRules())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        submitSjpProsecutionResponse.ifPresent(this::assertFailedBusinessValidationRules);
    }

    @Test
    public void businessValidationsShouldFailForGender() {
        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .defendant(getDefendantWithFailedBusinessValidationRulesForGenderValidation())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        submitSjpProsecutionResponse.ifPresent(this::assertFailedBusinessValidationRulesForGender);
    }

    @Test
    public void businessValidationsShouldFailV2() {
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(OUCODE)
                .build();

        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getDefendantWithFailedBusinessValidationRules())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        submitSjpProsecutionResponse.ifPresent(this::assertFailedBusinessValidationRules);
    }


    @Test
    public void businessValidationsShouldFailV2ForGenderValidation() {
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(OUCODE)
                .build();

        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .defendant(getDefendantWithFailedBusinessValidationRulesForGenderValidation())
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        submitSjpProsecutionResponse.ifPresent(this::assertFailedBusinessValidationRulesForGender);
    }

    private Defendant getDefendantWithFailedBusinessValidationRules() {
        final Offence offence = Offence.builder().build();

        final Address address = Address.builder()
                .address1("address1")
                .postcode("M60 1NW")
                .build();

        final ContactDetails contactDetails = ContactDetails.builder()
                .homeTelephoneNumber("44123")
                .mobileTelephoneNumber("44343")
                .workTelephoneNumber("41123")
                .build();

        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.builder()
                .gender(3)
                .build();

        final Person person = Person.builder()
                .title("Mrs")
                .forename("John2")
                .nationalInsuranceNumber("AB123456B")
                .selfDefinedInformation(selfDefinedInformation)
                .contactDetails(contactDetails)
                .address(address)
                .build();

        return Defendant.builder()
                .defendantPerson(person)
                .organisation(null)
                .offences(new Offence[]{offence})
                .build();
    }

    private Defendant getDefendantWithFailedBusinessValidationRulesForGenderValidation() {
        final Offence offence = Offence.builder().build();

        final Address address = Address.builder()
                .address1("address1")
                .postcode("M60 1NW")
                .build();

        final ContactDetails contactDetails = ContactDetails.builder()
                .homeTelephoneNumber("1234567890")
                .mobileTelephoneNumber("3234567890")
                .workTelephoneNumber("5234567890")
                .build();

        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.builder()
                .gender(3)
                .build();

        final Person person = Person.builder()
                .title("Mrs")
                .forename("John2")
                .nationalInsuranceNumber("AB123456B")
                .selfDefinedInformation(selfDefinedInformation)
                .contactDetails(contactDetails)
                .address(address)
                .build();

        return Defendant.builder()
                .defendantPerson(person)
                .organisation(null)
                .offences(new Offence[]{offence})
                .build();
    }

    private void assertFailedBusinessValidationRules(final Response submitSjpProsecutionResponse) {
        assertEquals(400, submitSjpProsecutionResponse.getStatus());

        final String response = submitSjpProsecutionResponse.readEntity(String.class);

        assertTrue(response.contains(FIELD_HOME_NUMBER));
        assertTrue(response.contains(FIELD_MOBILE_NUMBER));
        assertTrue(response.contains(FIELD_WORK_NUMBER));
        assertTrue(response.contains("string [44343] does not match pattern"));
        assertTrue(response.contains("string [44123] does not match pattern"));
        assertTrue(response.contains("string [41123] does not match pattern"));
    }

    private void assertFailedBusinessValidationRulesForGender(final Response submitSjpProsecutionResponse) {
        assertEquals(400, submitSjpProsecutionResponse.getStatus());

        final String response = submitSjpProsecutionResponse.readEntity(String.class);
        final JsonObject responseJson = stringToJsonObject(response);
        final JsonObject errors = stringToJsonObject(responseJson.getString("error"));

        final String genderError = errors.getJsonArray(BusinessValidationsTextResources.FIELD_GENDER).getString(0);

        assertEquals(GENDER_NOT_ACCEPTABLE, genderError);
    }

    private JsonObject stringToJsonObject(String response) {
        try (StringReader reader = new StringReader(response)) {
            return Json.createReader(reader).readObject();
        }
    }
}
