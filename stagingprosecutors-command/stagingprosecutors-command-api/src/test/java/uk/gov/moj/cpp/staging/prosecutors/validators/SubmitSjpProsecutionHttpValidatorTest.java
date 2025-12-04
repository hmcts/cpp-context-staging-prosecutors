package uk.gov.moj.cpp.staging.prosecutors.validators;

import static java.time.LocalDate.now;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpOffence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpPerson;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubmitSjpProsecutionHttpValidatorTest {

    @InjectMocks
    private SubmitSjpProsecutionHttpValidator submitSjpProsecutionHttpValidator;

    @Test
    public void validateInvalidGenderPayload() {
        final SubmitSjpProsecutionHttp submitSjpProsecutionHttp = givenPayloadWithInvalidGender();
        final Map<String, List<String>> actualValidations = whenSjpProsecutionValidatorIsInvokedWith(submitSjpProsecutionHttp);
        thenValidationFailsWith(actualValidations, "defendant.defendantPerson.selfDefinedInformation.gender", "Gender of the defendant should have one of the values 0, 1, 2 or 9");
    }

    @Test
    public void validateValidPayload() {
        final SubmitSjpProsecutionHttp submitSjpProsecutionHttp = givenPayloadWithValidContent();
        final Map<String, List<String>> actualValidations = whenSjpProsecutionValidatorIsInvokedWith(submitSjpProsecutionHttp);
        assertThat(actualValidations, equalTo(Collections.EMPTY_MAP));
    }


    @Test
    public void validateValidPayloadWithSameCommittedDateAndCommittedEndDate() {
        final SubmitSjpProsecutionHttp submitSjpProsecutionHttp = givenPayloadWithSameCommittedDateAndCommittedEndDate();
        final Map<String, List<String>> actualValidations = whenSjpProsecutionValidatorIsInvokedWith(submitSjpProsecutionHttp);
        assertThat(actualValidations, equalTo(Collections.EMPTY_MAP));
    }

    @Test
    public void validateInvalidDateCode4Payload() {
        final SubmitSjpProsecutionHttp submitSjpProsecutionHttp = givenOffenceDateCodeIs4AndOffenceCommittedEndDateIsNotSet();
        final Map<String, List<String>> actualValidations = whenSjpProsecutionValidatorIsInvokedWith(submitSjpProsecutionHttp);
        thenValidationFailsWith(actualValidations, "defendant.offences[0].offenceDateCode", "Cannot enter empty or invalid offence committed date and offence committed end date when offence date code is 4");
    }

    @Test
    public void validateInvalidCommittedDate() {
        final SubmitSjpProsecutionHttp submitSjpProsecutionHttp = givenOffenceDateCodeIs1AndOffenceCommittedDateIsNotSet();
        final Map<String, List<String>> actualValidations = whenSjpProsecutionValidatorIsInvokedWith(submitSjpProsecutionHttp);
        thenValidationFailsWith(actualValidations, "defendant.offences[0].offenceDateCode", "Cannot enter empty or invalid offence committed date when offence date code is 1, 2, 3, 5 or 6");
    }

    @Test
    public void validateOffenceSequenceNumberIsUnique() {
        final SubmitSjpProsecutionHttp submitSjpProsecutionHttp = givenPayloadWithTwoSetsOfOffencesWithDuplicateOffenceSequenceId();
        final Map<String, List<String>> actualValidations = whenSjpProsecutionValidatorIsInvokedWith(submitSjpProsecutionHttp);
        thenValidationFailsWith(actualValidations, "defendant.offences.offenceSequenceNo", "The offences [OFFENCE_CODE_1, OFFENCE_CODE_2], [OFFENCE_CODE_3, OFFENCE_CODE_4] have same offence sequence numbers. Offence sequence numbers must be unique.");
    }

    private SubmitSjpProsecutionHttp givenOffenceDateCodeIs1AndOffenceCommittedDateIsNotSet() {
        final SjpOffence offence = offenceWithDefaults().withOffenceDateCode(1).withOffenceCommittedDate(null).build();
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withGender(1).build();
        return buildSubmitSjpProsecutionHttp(offence, selfDefinedInformation);
    }

    private SubmitSjpProsecutionHttp givenPayloadWithInvalidGender() {
        final SjpOffence offence = offenceWithDefaults().build();
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withGender(6).build();
        return buildSubmitSjpProsecutionHttp(offence, selfDefinedInformation);
    }

    private SubmitSjpProsecutionHttp givenOffenceDateCodeIs4AndOffenceCommittedEndDateIsNotSet() {
        final SjpOffence offence = offenceWithDefaults().withOffenceDateCode(4).withOffenceCommittedDate(now()).withOffenceCommittedEndDate(null).build();
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withGender(1).build();
        return buildSubmitSjpProsecutionHttp(offence, selfDefinedInformation);
    }

    private SubmitSjpProsecutionHttp givenPayloadWithInvalidPhoneNumber() {
        final SjpOffence offence = offenceWithDefaults().withOffenceCommittedDate(now()).build();
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withGender(1).build();
        final ContactDetails contactDetails = ContactDetails.contactDetails().withHomeTelephoneNumber("4431233").withMobileTelephoneNumber(null).withWorkTelephoneNumber(null).build();
        final SjpPerson defendantPerson = SjpPerson.sjpPerson().withSelfDefinedInformation(selfDefinedInformation).withContactDetails(contactDetails).build();
        final SjpDefendant defendant = SjpDefendant.sjpDefendant().withDefendantPerson(defendantPerson).withOffences(Collections.singletonList(offence)).build();
        return SubmitSjpProsecutionHttp.submitSjpProsecutionHttp().withDefendant(defendant).build();
    }

    private SubmitSjpProsecutionHttp givenPayloadWithValidContent() {
        final SjpOffence offence = offenceWithDefaults().build();
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withGender(1).build();
        final ContactDetails contactDetails = ContactDetails.contactDetails().withHomeTelephoneNumber("1234121212").withMobileTelephoneNumber(null).withWorkTelephoneNumber(null).build();
        final SjpPerson defendantPerson = SjpPerson.sjpPerson().withSelfDefinedInformation(selfDefinedInformation).withContactDetails(contactDetails).build();
        final SjpDefendant defendant = SjpDefendant.sjpDefendant().withDefendantPerson(defendantPerson).withOffences(Collections.singletonList(offence)).build();
        return SubmitSjpProsecutionHttp.submitSjpProsecutionHttp().withDefendant(defendant).build();
    }

    private SubmitSjpProsecutionHttp givenPayloadWithTwoSetsOfOffencesWithDuplicateOffenceSequenceId() {
        final SjpOffence offence1 = offenceWithDefaults().withOffenceSequenceNo(1).withCjsOffenceCode("OFFENCE_CODE_1").build();
        final SjpOffence offence2 = offenceWithDefaults().withOffenceSequenceNo(1).withCjsOffenceCode("OFFENCE_CODE_2").build();
        final SjpOffence offence3 = offenceWithDefaults().withOffenceSequenceNo(2).withCjsOffenceCode("OFFENCE_CODE_3").build();
        final SjpOffence offence4 = offenceWithDefaults().withOffenceSequenceNo(2).withCjsOffenceCode("OFFENCE_CODE_4").build();
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withGender(1).build();
        final ContactDetails contactDetails = ContactDetails.contactDetails().withHomeTelephoneNumber("1234121212").withMobileTelephoneNumber(null).withWorkTelephoneNumber(null).build();
        final SjpPerson defendantPerson = SjpPerson.sjpPerson().withSelfDefinedInformation(selfDefinedInformation).withContactDetails(contactDetails).build();
        final SjpDefendant defendant = SjpDefendant.sjpDefendant().withDefendantPerson(defendantPerson).withOffences(Arrays.asList(offence1, offence2, offence3, offence4)).build();
        return SubmitSjpProsecutionHttp.submitSjpProsecutionHttp().withDefendant(defendant).build();
    }

    private SubmitSjpProsecutionHttp givenPayloadWithSameCommittedDateAndCommittedEndDate() {
        final SjpOffence offence = offenceWithDefaults().withOffenceCommittedDate(now()).withOffenceCommittedEndDate(now()).build();
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withGender(1).build();
        final ContactDetails contactDetails = ContactDetails.contactDetails().withHomeTelephoneNumber("1234121212").withMobileTelephoneNumber(null).withWorkTelephoneNumber(null).build();
        final SjpPerson defendantPerson = SjpPerson.sjpPerson().withSelfDefinedInformation(selfDefinedInformation).withContactDetails(contactDetails).build();
        final SjpDefendant defendant = SjpDefendant.sjpDefendant().withDefendantPerson(defendantPerson).withOffences(Collections.singletonList(offence)).build();
        return SubmitSjpProsecutionHttp.submitSjpProsecutionHttp().withDefendant(defendant).build();
    }

    private Map<String, List<String>> whenSjpProsecutionValidatorIsInvokedWith(final SubmitSjpProsecutionHttp submitSjpProsecutionHttp) {
        return submitSjpProsecutionHttpValidator.validate(submitSjpProsecutionHttp);
    }

    private SubmitSjpProsecutionHttp buildSubmitSjpProsecutionHttp(final SjpOffence offence, final SelfDefinedInformation selfDefinedInformation) {
        final SjpPerson defendantPerson = SjpPerson.sjpPerson().withSelfDefinedInformation(selfDefinedInformation).withContactDetails(null).build();
        final SjpDefendant defendant = SjpDefendant.sjpDefendant().withDefendantPerson(defendantPerson).withOffences(Collections.singletonList(offence)).build();
        return SubmitSjpProsecutionHttp.submitSjpProsecutionHttp().withDefendant(defendant).build();
    }

    private void thenValidationFailsWith(final Map<String, List<String>> actualValidations, final String errorCode, final String errorMessage) {
        final Map<String, List<String>> violationsMap = new HashMap<>();
        violationsMap.put(errorCode, Collections.singletonList(errorMessage));
        assertThat(actualValidations, equalTo(violationsMap));
    }

    private SjpOffence.Builder offenceWithDefaults() {
        int offenceSequenceNo = RandomUtils.nextInt(1, 100000);
        return SjpOffence.sjpOffence()
                .withOffenceSequenceNo(offenceSequenceNo)
                .withCjsOffenceCode("OFFENCE_CODE_" + offenceSequenceNo)
                .withOffenceDateCode(1)
                .withOffenceCommittedDate(now());
    }
}