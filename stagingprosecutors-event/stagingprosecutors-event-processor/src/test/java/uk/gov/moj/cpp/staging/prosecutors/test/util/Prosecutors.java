package uk.gov.moj.cpp.staging.prosecutors.test.util;

import static java.time.LocalDate.now;
import static java.time.LocalDate.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address.address;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails.contactDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails.defendantDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDetails.hearingDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.NameDetails.nameDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDetails.offenceDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardian.parentGuardian;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianIndividual.parentGuardianIndividual;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianNameDetails.parentGuardianNameDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianOrganisation.parentGuardianOrganisation;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionReceived.prosecutionReceived;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails.prosecutionSubmissionDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SelfDefinedInformation.selfDefinedInformation;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpDefendant.sjpDefendant;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpOffence.sjpOffence;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpPerson.sjpPerson;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionReceived.sjpProsecutionReceived;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails.sjpProsecutionSubmissionDetails;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.AlcoholRelatedOffence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Gender;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.InitiationCode;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Language;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.NameDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDateCode;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Organisation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardian;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpOffence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpOrganisation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpPerson;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class Prosecutors {

    public static final LocalDate SJPN_POSTING_DATE = parse("2018-06-01");
    public static final String URN = "TVL123456";
    public static final String PROSECUTING_AUTHORITY = "TVL";
    public static final UUID SUBMISSION_ID = fromString("be5bb607-0e98-43a3-93da-123741c6f73d");
    private static final LocalDate DATE_OF_BIRTH = now().minusYears(21);

    public static List<SjpOffence> prosecutorsSjpOffenceList(final int numberOfElements) {
        return rangeClosed(1, numberOfElements)
                .mapToObj(Prosecutors::prosecutorsSjpOffence)
                .collect(toList());
    }

    public static SjpOffence prosecutorsSjpOffence() {
        return prosecutorsSjpOffence(1);
    }

    public static SjpOffence prosecutorsSjpOffence(final int sequenceNo) {

        return sjpOffence()
                .withBackDuty("12.33")//NOT IN ATCM
                .withBackDutyDateFrom(parse("2018-02-02"))//NOT IN ATCM
                .withBackDutyDateTo(parse("2018-02-20"))//NOT IN ATCM
                .withChargeDate(parse("2018-03-20"))//CHECKED
                .withCjsOffenceCode("PS0000" + sequenceNo)//CHECKED
                .withOffenceCommittedDate(parse("2018-02-25"))//CHECKED
                .withOffenceCommittedEndDate(parse("2018-02-25"))//NOT IN ATCM
                .withOffenceDateCode(sequenceNo + 10)//NOT IN ATCM
                .withOffenceLocation("offenceLocation")//NOT IN ATCM
                .withOffenceSequenceNo(sequenceNo)//CHECKED
                .withOffenceWording("offenceWording")//CHECKED
                .withOffenceWordingWelsh("offenceWordingWelsh")//NOT IN ATCM
                .withStatementOfFacts("statementOfFacts")//CHECKED
                .withStatementOfFactsWelsh("statementOfFactsWelsh")//CHECKED
                .withProsecutorCompensation("23.23")//CHECKED
                .withVehicleMake("Ford")
                .withVehicleRegistrationMark("AA11 ABC")
                .withProsecutorOfferAOCP(true)
                .build();
    }

    public static List<Offence> prosecutorsOffenceList(final int numberOfElements) {
        return rangeClosed(1, numberOfElements)
                .mapToObj(Prosecutors::prosecutorsOffence)
                .collect(toList());
    }

    public static Offence prosecutorsOffence(final int sequenceNo) {

        return Offence.offence()
                .withOffenceDetails(offenceDetails()
                        .withAlcoholRelatedOffence(AlcoholRelatedOffence.alcoholRelatedOffence().withAlcoholOrDrugLevelAmount(10)
                                .withAlcoholOrDrugLevelMethod(("Method")).build())
                        .withBackDuty("12.33")
                        .withBackDutyDateFrom(parse("2018-02-02"))
                        .withBackDutyDateTo(parse("2018-02-20"))
                        .withCjsOffenceCode("PS0000" + sequenceNo)
                        .withOffenceCommittedDate(parse("2018-02-25"))
                        .withOffenceCommittedEndDate(parse("2018-02-25"))
                        .withOffenceDateCode(OffenceDateCode.NUMBER_1)
                        .withOffenceLocation("offenceLocation")
                        .withOffenceSequenceNo(sequenceNo)
                        .withOffenceWording("offenceWording")
                        .withOffenceWordingWelsh("offenceWordingWelsh")
                        .withProsecutorCompensation("23.23")
                        .withVehicleMake("Ford")
                        .withVehicleRegistrationMark("AA11 ABC")
                        .build())
                .withChargeDate(parse("2018-02-02"))
                .withStatementOfFacts("Statement of facts")
                .withStatementOfFactsWelsh("Statement of facts welsh")
                .withArrestDate(parse("2018-02-02"))
                .build();

    }

    public static ContactDetails prosecutorsContactDetails() {
        return contactDetails()
                .withPrimaryEmail("address1@email.com")
                .withSecondaryEmail("address2@email.com")
                .withWorkTelephoneNumber("02012345678")
                .withHomeTelephoneNumber("02087654321")
                .withMobileTelephoneNumber("0789123456")
                .build();
    }

    public static Address prosecutorsSjpAddress() {
        return address()
                .withAddress1("address1") //CHECKED
                .withAddress2("address2") //CHECKED
                .withAddress3("address3") //CHECKED
                .withAddress4("address4") //CHECKED
                .withAddress5("address5") //NOT IN ATCM - added by Ivo
                .withPostcode("postcode") //CHECKED
                .build();
    }

    public static Address prosecutorsAddress() {
        return address()
                .withAddress1("address1") //CHECKED
                .withAddress2("address2") //CHECKED
                .withAddress3("address3") //CHECKED
                .withAddress4("address4") //CHECKED
                .withAddress5("address5") //NOT IN ATCM - added by Ivo
                .withPostcode("postc ode") //CHECKED
                .build();
    }

    public static Address prosecutorsSjpAddressWithoutPostcode() {
        return address()
                .withAddress1("address1") //CHECKED
                .withAddress2("address2") //CHECKED
                .withAddress3("address3") //CHECKED
                .withAddress4("address4") //CHECKED
                .withAddress5("address5") //NOT IN ATCM - added by Ivo
                .withPostcode(null)
                .build();
    }


    public static SelfDefinedInformation prosecutorsSelfDefinedInformation() {
        return selfDefinedInformation()
                .withGender(1) //MAGIC NUMBER FOR GENDER DEFINED IN CJS DATA STANDARDS...
                .withEthnicity("W1")
                .build();
    }

    public static SjpPerson prosecutorsSjpPerson() {
        return sjpPerson()
                .withTitle("Mr") //CHECKED
                .withForename("Adam") //CHECKED
                .withForename2("forename2") //NOT IN ATCM - added by Ivo
                .withForename3("forename3") //NOT IN ATCM - added by Ivo
                .withSurname("SMITH")//CHECKED
                .withDateOfBirth(DATE_OF_BIRTH) //CHECKED
                .withSelfDefinedInformation(prosecutorsSelfDefinedInformation())
                .withAddress(prosecutorsSjpAddress())
                .withContactDetails(prosecutorsContactDetails())
                .withNationalInsuranceNumber("AA123456C") //NOT IN ATCM
                .withDriverNumber("driverNumber") //NOT IN ATCM
                .withOccupation("occupation") //NOT IN ATCM
                .withOccupationCode(123) //NOT IN ATCM
                .build();
    }

    public static SjpOrganisation prosecutorsSjpOrganisation() {
        return  SjpOrganisation.sjpOrganisation()
                .withOrganisationName("ABC org")
                .withAddress(prosecutorsSjpAddress())
                .withContactDetails(contactDetails().withPrimaryEmail("xyz@gmail.com")
                        .withWorkTelephoneNumber("07551239555")
                        .build())

                .build();
    }

    public static SjpPerson prosecutorsSjpPersonWithoutPostcode() {
        return sjpPerson()
                .withTitle("Mr") //CHECKED
                .withForename("Adam") //CHECKED
                .withForename2("forename2") //NOT IN ATCM - added by Ivo
                .withForename3("forename3") //NOT IN ATCM - added by Ivo
                .withSurname("SMITH")//CHECKED
                .withDateOfBirth(DATE_OF_BIRTH)//CHECKED
                .withSelfDefinedInformation(prosecutorsSelfDefinedInformation())
                .withAddress(prosecutorsSjpAddressWithoutPostcode())
                .withContactDetails(prosecutorsContactDetails())
                .withNationalInsuranceNumber("AA123456C") //NOT IN ATCM
                .withDriverNumber("driverNumber") //NOT IN ATCM
                .withOccupation("occupation") //NOT IN ATCM
                .withOccupationCode(123) //NOT IN ATCM
                .build();
    }

    public static Individual prosecutorsIndividualWithParentGuardianIndividual() {
        return prosecutorsIndividual()
                .withParentGuardian(prosecutorsParentGuardianIndividual())
                .build();
    }

    public static Individual prosecutorsIndividualWithParentGuardianOrganisation() {
        return prosecutorsIndividual()
                .withParentGuardian(prosecutorsParentGuardianOrganisation())
                .build();
    }

    private static Individual.Builder prosecutorsIndividual() {
        return individual()
//                .withAdditionalNationality("NATIONALITY"))
                .withAliases(asList(prosecutorsNameDetails(), prosecutorsNameDetails()))
                .withContactDetails(prosecutorsContactDetails())
                .withDateOfBirth(LocalDate.now())
                .withDriverNumber("Driver Number")
                .withEthnicity("Ethnicity")
                .withGender(Gender.NUMBER_1)
                .withNameDetails(prosecutorsNameDetails())
                .withNationalInsuranceNumber("National Insurance Number")
//                .withNationality("Nationality"))
                .withObservedEthnicity(BigDecimal.ONE)
                .withOccupation("Occupation")
                .withCustodyStatus("E")
                .withBailConditions("BAIL CONDITIONS")
                .withLanguageRequirement("languageNeeds")
                .withSpecificRequirements("specialNeeds")
                .withParentGuardian(null)
                .withOccupationCode(1);
    }

    private static NameDetails prosecutorsNameDetails() {
        return nameDetails()
                .withForename("Adam")
                .withForename2("forename2")
                .withForename3("forename3")
                .withSurname("SMITH")
                .withTitle("Mr")
                .build();
    }

    private static ParentGuardian prosecutorsParentGuardianIndividual() {
        return parentGuardian()
                .withAddress(prosecutorsAddress())
                .withIndividual(parentGuardianIndividual()
                        .withContactDetails(prosecutorsContactDetails())
                        .withDateOfBirth(LocalDate.now())
                        .withGender(Gender.NUMBER_2)
                        .withNameDetails(parentGuardianNameDetails()
                                .withForename("Forename")
                                .withForename2("Forename2")
                                .withForename3("Forename3")
                                .withSurname("Surname")
                                .withTitle("Mr")
                                .build())
                        .withObservedEthnicity(BigDecimal.ONE)
                        .withSelfDefinedEthnicity("Self Define Ethnicity")
                        .build())
                .build();
    }

    private static ParentGuardian prosecutorsParentGuardianOrganisation() {
        return parentGuardian()
                .withAddress(prosecutorsAddress())
                .withOrganisation(parentGuardianOrganisation()
                        .withOrganisationName("Organisation Name")
                        .withCompanyTelephoneNumber("1111111111")
                        .build())
                .withIndividual(null)
                .build();
    }

    public static SjpDefendant prosecutorsSjpDefendant() {
        return sjpDefendant()
                .withDefendantPerson(prosecutorsSjpPerson())
                .withNumPreviousConvictions(3) //CHECKED
                .withProsecutorCosts("30.12") //CHECKED
                .withAsn("asn")//NOT IN ATCM
                .withDocumentationLanguage(Language.E) //NOT IN ATCM
                .withHearingLanguage(Language.E)//NOT IN ATCM
                .withLanguageRequirement("languageNeeds")//NOT IN ATCM
                .withOffences(prosecutorsSjpOffenceList(1))
                .withSpecificRequirements("specialNeeds")//NOT IN ATCM
                .build();
    }

    public static SjpDefendant prosecutorsSjpDefendantOrganisation() {
        return sjpDefendant().withOrganisation(prosecutorsSjpOrganisation())
                .withNumPreviousConvictions(3) //CHECKED
                .withProsecutorCosts("30.12") //CHECKED
                .withAsn("asn")//NOT IN ATCM
                .withDocumentationLanguage(Language.E) //NOT IN ATCM
                .withHearingLanguage(Language.E)//NOT IN ATCM
                .withLanguageRequirement("languageNeeds")//NOT IN ATCM
                .withOffences(prosecutorsSjpOffenceList(1))
                .withSpecificRequirements("specialNeeds")//NOT IN ATCM
                .build();
    }

    public static Defendant prosecutorsDefendant() {
        return Defendant.defendant()
                .withDefendantDetails(defendantDetails()
                        .withAddress(prosecutorsAddress())
                        .withAsn("asn")
                        .withCroNumber("CroNumber")
                        .withDocumentationLanguage(Language.E)
                        .withHearingLanguage(Language.E)
                        .withNumPreviousConvictions(1)
                        .withPncIdentifier("PnCidentifier")
                        .withProsecutorCosts(BigDecimal.TEN.toString())
                        .withProsecutorDefendantId(randomUUID().toString())
                        .build())
                .withOffences(singletonList(prosecutorsOffence(1)))
                .withIndividual(prosecutorsIndividual().build())
                .withOrganisation(Organisation.organisation()
                        .withOrganisationName("Organisation Name")
                        .withCompanyTelephoneNumber("12323453456")
                        .withAliasOrganisationNames(asList("Alias1", "Alias2", "Alias3"))
                        .build())
                .build();
    }

    public static SjpProsecutionSubmissionDetails prosecutorsSjpProsecutionSubmissionDetails() {
        return sjpProsecutionSubmissionDetails()
                .withUrn(URN) //CHECKED
                .withInformant("informant") //NOT IN ATCM
                .withProsecutingAuthority(PROSECUTING_AUTHORITY) //CHECKED
                .withWrittenChargePostingDate(SJPN_POSTING_DATE) //CHECKED
                .build();
    }

    private static ProsecutionSubmissionDetails prosecutorsProsecutionSubmissionDetails() {
        return prosecutionSubmissionDetails()
                .withUrn(URN)
                .withInformant("informant")
                .withProsecutingAuthority(PROSECUTING_AUTHORITY)
                .withWrittenChargePostingDate(LocalDate.now())
                .withInitiationCode(InitiationCode.S)
                .withCaseMarker("ABC")
                .withSummonsCode("E")
                .withHearingDetails(hearingDetails()
                        .withDateOfHearing(LocalDate.now())
                        .withTimeOfHearing("Time Of Hearing")
                        .withCourtHearingLocation("Court Hearing Location")
                        .build())
                .build();
    }

    public static SjpProsecutionReceived prosecutorsSjpProsecutionReceived() {
        return sjpProsecutionReceived()
                .withSubmissionId(SUBMISSION_ID)
                .withProsecutionSubmissionDetails(prosecutorsSjpProsecutionSubmissionDetails())
                .withDefendant(prosecutorsSjpDefendant())
                .build();
    }

    public static ProsecutionReceived prosecutorsProsecutionReceived() {
        return prosecutionReceived()
                .withSubmissionId(SUBMISSION_ID)
                .withProsecutionSubmissionDetails(prosecutorsProsecutionSubmissionDetails())
                .withDefendants(singletonList(prosecutorsDefendant()))
                .build();
    }
}
