package uk.gov.moj.cpp.staging.prosecutors.validators;

import static java.time.LocalDate.now;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpOffence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpPerson;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttpV2;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubmitSjpProsecutionHttpV2ValidatorTest {

    @InjectMocks
    private SubmitSjpProsecutionHttpV2Validator submitSjpProsecutionHttpV2Validator;

    @Test
    public void validateInvalidGenderPayload() {
        final SubmitSjpProsecutionHttpV2 submitSjpProsecutionHttpV2 = givenPayloadWithInvalidGender();
        final Map<String, List<String>> actualValidations = whenSjpProsecutionValidatorIsInvokedWith(submitSjpProsecutionHttpV2);
        thenValidationFailsWith(actualValidations, "defendant.defendantPerson.selfDefinedInformation.gender", "Gender of the defendant should have one of the values 0, 1, 2 or 9");
    }

    @Test
    public void validateValidPayload() {
        final SubmitSjpProsecutionHttpV2 submitSjpProsecutionHttpV2 = givenPayloadWithValidContent();
        final Map<String, List<String>> actualValidations = whenSjpProsecutionValidatorIsInvokedWith(submitSjpProsecutionHttpV2);
        assertThat(actualValidations, equalTo(Collections.EMPTY_MAP));
    }


    @Test
    public void validateValidPayloadWithSameCommittedDateAndCommittedEndDate() {
        final SubmitSjpProsecutionHttpV2 submitSjpProsecutionHttpV2 = givenPayloadWithSameCommittedDateAndCommittedEndDate();
        final Map<String, List<String>> actualValidations = whenSjpProsecutionValidatorIsInvokedWith(submitSjpProsecutionHttpV2);
        assertThat(actualValidations, equalTo(Collections.EMPTY_MAP));
    }

    @Test
    public void validateInvalidDateCode4Payload() {
        final SubmitSjpProsecutionHttpV2 submitSjpProsecutionHttpV2 = givenOffenceDateCodeIs4AndOffenceCommittedEndDateIsNotSet();
        final Map<String, List<String>> actualValidations = whenSjpProsecutionValidatorIsInvokedWith(submitSjpProsecutionHttpV2);
        thenValidationFailsWith(actualValidations, "defendant.offences[0].offenceDateCode", "Cannot enter empty or invalid offence committed date and offence committed end date when offence date code is 4");
    }

    @Test
    public void validateInvalidCommittedDate(){
        final SubmitSjpProsecutionHttpV2 submitSjpProsecutionHttpV2 = givenOffenceDateCodeIs1AndOffenceCommittedDateIsNotSet();
        final Map<String, List<String>> actualValidations = whenSjpProsecutionValidatorIsInvokedWith(submitSjpProsecutionHttpV2);
        thenValidationFailsWith(actualValidations, "defendant.offences[0].offenceDateCode", "Cannot enter empty or invalid offence committed date when offence date code is 1, 2, 3, 5 or 6");
    }

    private SubmitSjpProsecutionHttpV2 givenOffenceDateCodeIs1AndOffenceCommittedDateIsNotSet() {
        final SjpOffence offence = SjpOffence.sjpOffence().withOffenceDateCode(1).build();
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withGender(1).build();
        return buildSubmitSjpProsecutionHttp(offence, selfDefinedInformation);
    }

    private SubmitSjpProsecutionHttpV2 givenPayloadWithInvalidGender() {
        final SjpOffence offence = SjpOffence.sjpOffence().withOffenceDateCode(1).withOffenceCommittedDate(now()).build();
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withGender(6).build();
        return buildSubmitSjpProsecutionHttp(offence, selfDefinedInformation);
    }

    private SubmitSjpProsecutionHttpV2 givenOffenceDateCodeIs4AndOffenceCommittedEndDateIsNotSet() {
        final SjpOffence offence = SjpOffence.sjpOffence().withOffenceDateCode(4).withOffenceCommittedDate(now()).withOffenceCommittedEndDate(null).build();
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withGender(1).build();
        return buildSubmitSjpProsecutionHttp(offence, selfDefinedInformation);
    }

    private SubmitSjpProsecutionHttpV2 givenPayloadWithInvalidPhoneNumber(final String phoneNumber) {
        final SjpOffence offence = SjpOffence.sjpOffence().withOffenceDateCode(1).withOffenceCommittedDate(now()).build();
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withGender(1).build();
        final ContactDetails contactDetails = ContactDetails.contactDetails().withHomeTelephoneNumber(phoneNumber).withMobileTelephoneNumber(null).withWorkTelephoneNumber(null).build();
        final SjpPerson defendantPerson = SjpPerson.sjpPerson().withSelfDefinedInformation(selfDefinedInformation).withContactDetails(contactDetails).build();
        final SjpDefendant defendant = SjpDefendant.sjpDefendant().withDefendantPerson(defendantPerson).withOffences(Collections.singletonList(offence)).build();
        return SubmitSjpProsecutionHttpV2.submitSjpProsecutionHttpV2().withDefendant(defendant).build();
    }

    private SubmitSjpProsecutionHttpV2 givenPayloadWithValidContent() {
        final SjpOffence offence = SjpOffence.sjpOffence().withOffenceDateCode(1).withOffenceCommittedDate(now()).build();
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withGender(1).build();
        final ContactDetails contactDetails = ContactDetails.contactDetails().withHomeTelephoneNumber("1234121212").withMobileTelephoneNumber(null).withWorkTelephoneNumber(null).build();
        final SjpPerson defendantPerson = SjpPerson.sjpPerson().withSelfDefinedInformation(selfDefinedInformation).withContactDetails(contactDetails).build();
        final SjpDefendant defendant = SjpDefendant.sjpDefendant().withDefendantPerson(defendantPerson).withOffences(Collections.singletonList(offence)).build();
        return SubmitSjpProsecutionHttpV2.submitSjpProsecutionHttpV2().withDefendant(defendant).build();
    }


    private SubmitSjpProsecutionHttpV2 givenPayloadWithSameCommittedDateAndCommittedEndDate() {
        final SjpOffence offence = SjpOffence.sjpOffence().withOffenceDateCode(4).withOffenceCommittedDate(now()).withOffenceCommittedEndDate(now()).build();
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withGender(1).build();
        final ContactDetails contactDetails = ContactDetails.contactDetails().withHomeTelephoneNumber("1234121212").withMobileTelephoneNumber(null).withWorkTelephoneNumber(null).build();
        final SjpPerson defendantPerson = SjpPerson.sjpPerson().withSelfDefinedInformation(selfDefinedInformation).withContactDetails(contactDetails).build();
        final SjpDefendant defendant = SjpDefendant.sjpDefendant().withDefendantPerson(defendantPerson).withOffences(Collections.singletonList(offence)).build();
        return SubmitSjpProsecutionHttpV2.submitSjpProsecutionHttpV2().withDefendant(defendant).build();
    }

    private Map<String, List<String>> whenSjpProsecutionValidatorIsInvokedWith(final SubmitSjpProsecutionHttpV2 submitSjpProsecutionHttpV2) {
        return submitSjpProsecutionHttpV2Validator.validate(submitSjpProsecutionHttpV2);
    }

    private SubmitSjpProsecutionHttpV2 buildSubmitSjpProsecutionHttp(final SjpOffence offence, final SelfDefinedInformation selfDefinedInformation) {
        final SjpPerson defendantPerson = SjpPerson.sjpPerson().withSelfDefinedInformation(selfDefinedInformation).withContactDetails(null).build();
        final SjpDefendant defendant = SjpDefendant.sjpDefendant().withDefendantPerson(defendantPerson).withOffences(Collections.singletonList(offence)).build();
        return SubmitSjpProsecutionHttpV2.submitSjpProsecutionHttpV2().withDefendant(defendant).build();
    }

    private void thenValidationFailsWith(final Map<String, List<String>> actualValidations, final String errorCode, final String errorMessage) {
        final Map<String, List<String>> violationsMap = new HashMap<>();
        violationsMap.put(errorCode, Collections.singletonList(errorMessage));
        assertThat(actualValidations, equalTo(violationsMap));
    }

}