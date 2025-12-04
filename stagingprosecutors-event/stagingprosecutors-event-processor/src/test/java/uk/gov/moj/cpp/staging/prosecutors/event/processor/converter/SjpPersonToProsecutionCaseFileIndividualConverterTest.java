package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsSjpPerson;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsSjpPersonWithoutPostcode;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpPerson;

import org.junit.jupiter.api.Test;

public class SjpPersonToProsecutionCaseFileIndividualConverterTest {

    private static Converter<Integer, Gender> integerGenderToProsecutionCaseFileGenderConverter = new IntegerGenderToProsecutionCaseFileGenderConverter();


    @Test
    public void shouldConvertProsecutionPersonToProsecutionCaseFileIndividual() {
        final Converter<SjpPerson, Individual> converter = new SjpPersonToProsecutionCaseFileIndividualConverter();

        final SjpPerson prosecutorsPerson = prosecutorsSjpPerson();
        final Individual prosecutorCaseFileIndividual = converter.convert(prosecutorsPerson);

        assertProsecutionCaseFileIndividualMatchesProsecutionPerson(prosecutorCaseFileIndividual, prosecutorsPerson);
    }

    @Test
    public void shouldConvertProsecutionPersonToProsecutionCaseFileIndividual_WithoutPostcode() {
        final Converter<SjpPerson, Individual> converter = new SjpPersonToProsecutionCaseFileIndividualConverter();

        final SjpPerson prosecutorsPerson = prosecutorsSjpPersonWithoutPostcode();
        final Individual prosecutorCaseFileIndividual = converter.convert(prosecutorsPerson);

        assertProsecutionCaseFileIndividualMatchesProsecutionPerson(prosecutorCaseFileIndividual, prosecutorsPerson);
    }

    public static void assertProsecutionCaseFileIndividualMatchesProsecutionPerson(final Individual prosecutorCaseFileIndividual,
                                                                                   final SjpPerson prosecutorsPerson) {

        assertThat(prosecutorCaseFileIndividual.getDriverNumber(), is(prosecutorsPerson.getDriverNumber()));
        assertThat(prosecutorCaseFileIndividual.getNationalInsuranceNumber(), is(prosecutorsPerson.getNationalInsuranceNumber()));

        final PersonalInformation personalInformation = prosecutorCaseFileIndividual.getPersonalInformation();

        assertProsecutionCasePersonalInformationMatchesProsecutionPerson(prosecutorCaseFileIndividual.getPersonalInformation(), prosecutorsPerson);
        assertProsecutionCaseContactDetailsMatchesProsecutorPersonContactDetails(personalInformation.getContactDetails(), prosecutorsPerson);
        assertProsecutionCaseSelfDefinedInformationMatchesProsecutorPersonSelfDefinedInformation(prosecutorCaseFileIndividual.getSelfDefinedInformation(), prosecutorsPerson);
    }


    public static void assertProsecutionCaseSelfDefinedInformationMatchesProsecutorPersonSelfDefinedInformation(final SelfDefinedInformation selfDefinedInformation,
                                                                                                                final SjpPerson prosecutorsPerson) {
        assertThat(selfDefinedInformation.getAdditionalNationality(),
                is(isEmptyOrNullString()));
        assertThat(selfDefinedInformation.getEthnicity(), is(prosecutorsPerson.getSelfDefinedInformation().getEthnicity()));
        assertThat(selfDefinedInformation.getDateOfBirth(), is(prosecutorsPerson.getDateOfBirth()));
        assertThat(selfDefinedInformation.getNationality(), is(isEmptyOrNullString()));
        assertThat(selfDefinedInformation.getGender(),
                is(integerGenderToProsecutionCaseFileGenderConverter.convert(prosecutorsPerson.getSelfDefinedInformation().getGender())));
    }

    public static void assertProsecutionCaseContactDetailsMatchesProsecutorPersonContactDetails(final ContactDetails personalInformationContactDetails,
                                                                                                final SjpPerson prosecutorsPerson) {
        assertThat(personalInformationContactDetails.getHome(), is(ofNullable(prosecutorsPerson.getContactDetails())
                .map(cd -> cd.getHomeTelephoneNumber())
                .map(String::toString)
                .orElse(null)));
        assertThat(personalInformationContactDetails.getMobile(), is(ofNullable(prosecutorsPerson.getContactDetails())
                .map(cd -> cd.getMobileTelephoneNumber())
                .map(String::toString)
                .orElse(null)));
        assertThat(personalInformationContactDetails.getPrimaryEmail(), is(ofNullable(prosecutorsPerson.getContactDetails())
                .map(cd -> cd.getPrimaryEmail())
                .map(String::toString)
                .orElse(null)));
        assertThat(personalInformationContactDetails.getSecondaryEmail(), is(ofNullable(prosecutorsPerson.getContactDetails())
                .map(cd -> cd.getSecondaryEmail())
                .map(String::toString)
                .orElse(null)));
        assertThat(personalInformationContactDetails.getWork(), is(ofNullable(prosecutorsPerson.getContactDetails())
                .map(cd -> cd.getWorkTelephoneNumber())
                .map(String::toString)
                .orElse(null)));
    }


    public static void assertProsecutionCasePersonalInformationMatchesProsecutionPerson(final PersonalInformation personalInformation, final SjpPerson prosecutorsPerson) {
        final StringBuilder expectedFirstNameBuilder = new StringBuilder(prosecutorsPerson.getForename());
        ofNullable(prosecutorsPerson.getForename2()).ifPresent(forename2 -> expectedFirstNameBuilder.append(" ").append(forename2));
        ofNullable(prosecutorsPerson.getForename3()).ifPresent(forename3 -> expectedFirstNameBuilder.append(" ").append(forename3));
        assertThat(personalInformation.getFirstName(), is(expectedFirstNameBuilder.toString()));
        assertThat(personalInformation.getLastName(), is(prosecutorsPerson.getSurname()));
        assertThat(personalInformation.getTitle(), is(prosecutorsPerson.getTitle()));
        assertThat(personalInformation.getOccupation(), is(prosecutorsPerson.getOccupation()));
        assertThat(personalInformation.getOccupationCode(), is(prosecutorsPerson.getOccupationCode()));

        final Address personalInformationAddress = personalInformation.getAddress();
        assertThat(personalInformationAddress.getAddress1(), is(prosecutorsPerson.getAddress().getAddress1()));
        assertThat(personalInformationAddress.getAddress2(), is(prosecutorsPerson.getAddress().getAddress2()));
        assertThat(personalInformationAddress.getAddress3(), is(prosecutorsPerson.getAddress().getAddress3()));
        assertThat(personalInformationAddress.getAddress4(), is(prosecutorsPerson.getAddress().getAddress4()));
        assertThat(personalInformationAddress.getAddress5(), is(prosecutorsPerson.getAddress().getAddress5()));
        assertThat(personalInformationAddress.getPostcode(), is(prosecutorsPerson.getAddress().getPostcode()));
    }
}