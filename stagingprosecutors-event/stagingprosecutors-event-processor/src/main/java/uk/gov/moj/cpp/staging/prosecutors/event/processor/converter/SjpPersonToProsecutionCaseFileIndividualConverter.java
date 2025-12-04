package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address.address;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails.contactDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation.selfDefinedInformation;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpPerson;

import java.time.LocalDate;
import java.util.function.Function;

public class SjpPersonToProsecutionCaseFileIndividualConverter implements Converter<SjpPerson, Individual> {

    private final Converter<Integer, Gender> integerGenderToProsecutionCaseFileGenderConverter = new IntegerGenderToProsecutionCaseFileGenderConverter();

    private final Function<ContactDetails, uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails> convertContactDetailsFnc =
            prosecutionContactDetails ->
                    contactDetails()
                            .withHome(prosecutionContactDetails.getHomeTelephoneNumber())
                            .withMobile(prosecutionContactDetails.getMobileTelephoneNumber())
                            .withPrimaryEmail(prosecutionContactDetails.getPrimaryEmail())
                            .withSecondaryEmail(prosecutionContactDetails.getSecondaryEmail())
                            .withWork(prosecutionContactDetails.getWorkTelephoneNumber())
                            .build();

    @Override
    public Individual convert(final SjpPerson person) {
        return individual()
                .withDriverNumber(person.getDriverNumber())
                .withNationalInsuranceNumber(person.getNationalInsuranceNumber())
                .withPersonalInformation(personalInformation()
                        .withAddress(convert(person.getAddress()))
                        .withContactDetails(ofNullable(person.getContactDetails()).map(convertContactDetailsFnc).orElse(null))
                        .withFirstName(createForename(person))
                        .withLastName(person.getSurname())
                        .withOccupation(person.getOccupation())
                        .withOccupationCode(person.getOccupationCode())
                        .withTitle(person.getTitle())
                        .build())
                .withSelfDefinedInformation(convert(person.getSelfDefinedInformation(), person.getDateOfBirth()))
                .build();
    }


    private String createForename(final SjpPerson person) {
        final StringBuilder sb = new StringBuilder(person.getForename());
        ofNullable(person.getForename2()).ifPresent(forename2 -> sb.append(" ").append(forename2));
        ofNullable(person.getForename3()).ifPresent(forename3 -> sb.append(" ").append(forename3));
        return sb.toString();
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address convert(final Address address) {
        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address.Builder builder = address()
                .withAddress1(address.getAddress1());


        ofNullable(address.getAddress2()).ifPresent(builder::withAddress2);
        ofNullable(address.getAddress3()).ifPresent(builder::withAddress3);
        ofNullable(address.getAddress4()).ifPresent(builder::withAddress4);
        ofNullable(address.getAddress5()).ifPresent(builder::withAddress5);
        ofNullable(address.getPostcode()).ifPresent(builder::withPostcode);

        return builder.build();
    }


    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation convert(final SelfDefinedInformation selfDefinedInformation,
                                                                                            final LocalDate dateOfBirth) {
        return selfDefinedInformation()
                .withEthnicity(selfDefinedInformation.getEthnicity())
                .withGender(integerGenderToProsecutionCaseFileGenderConverter.convert(selfDefinedInformation.getGender()))
                .withDateOfBirth(dateOfBirth)
                .build();
    }
}
