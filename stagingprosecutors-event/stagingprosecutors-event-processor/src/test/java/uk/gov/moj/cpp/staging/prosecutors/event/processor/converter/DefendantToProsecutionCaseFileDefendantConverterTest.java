package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails.prosecutionSubmissionDetails;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsDefendant;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Language;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Organisation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class DefendantToProsecutionCaseFileDefendantConverterTest {

    @Test
    public void shouldConvertProsecutionDefendantToProsecutionCaseFileDefendant() {

        ProsecutionSubmissionDetails prosecutionSubmissionDetails = prosecutionSubmissionDetails()
                .withHearingDetails(HearingDetails.hearingDetails().withTimeOfHearing("Time of Hearing")
                        .withDateOfHearing(LocalDate.now())
                        .withCourtHearingLocation("Location")
                        .build())
                .withWrittenChargePostingDate(LocalDate.now())
                .build();

        final Converter<Defendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> converter
                = new DefendantToProsecutionCaseFileDefendantConverter(prosecutionSubmissionDetails);

        final Defendant prosecutorsDefendant = prosecutorsDefendant();


        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant prosecutionCaseFileDefendant = converter.convert(prosecutorsDefendant);

        assertProsecutionCaseFileDefendantMatchesProsecutionDefendant(prosecutionCaseFileDefendant, prosecutorsDefendant, prosecutionSubmissionDetails);
    }


    private static void assertProsecutionCaseFileDefendantMatchesProsecutionDefendant(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant prosecutionCaseFileDefendant,
                                                                                     final Defendant prosecutorsDefendant, final ProsecutionSubmissionDetails prosecutionSubmissionDetails) {

        assertThat(prosecutionCaseFileDefendant, is(notNullValue()));

        assertThat(prosecutionCaseFileDefendant.getAliasForCorporate(), is(ofNullable(prosecutorsDefendant.getOrganisation()).map(Organisation::getAliasOrganisationNames).orElse(null)));
        assertThat(prosecutionCaseFileDefendant.getAppliedProsecutorCosts(), is(ofNullable(prosecutorsDefendant.getDefendantDetails()).map(DefendantDetails::getProsecutorCosts).map(BigDecimal::new).orElse(null)));
        assertThat(prosecutionCaseFileDefendant.getAsn(), is(ofNullable(prosecutorsDefendant.getDefendantDetails()).map(DefendantDetails::getAsn).orElse(null)));
        assertThat(prosecutionCaseFileDefendant.getCroNumber(), is(ofNullable(prosecutorsDefendant.getDefendantDetails()).map(DefendantDetails::getCroNumber).orElse(null)));
        assertThat(prosecutionCaseFileDefendant.getDocumentationLanguage().name(), is(ofNullable(prosecutorsDefendant.getDefendantDetails()).map(DefendantDetails::getDocumentationLanguage).map(Language::name).orElse(null)));
        assertThat(prosecutionCaseFileDefendant.getHearingLanguage().name(), is(ofNullable(prosecutorsDefendant.getDefendantDetails()).map(DefendantDetails::getHearingLanguage).map(Language::name).orElse(null)));
        assertThat(prosecutionCaseFileDefendant.getOrganisationName(), is(ofNullable(prosecutorsDefendant.getOrganisation()).map(Organisation::getOrganisationName).orElse(null)));
        assertThat(prosecutionCaseFileDefendant.getTelephoneNumberBusiness(), is(ofNullable(prosecutorsDefendant.getOrganisation()).map(Organisation::getCompanyTelephoneNumber).orElse(null)));
        assertThat(prosecutionCaseFileDefendant.getPncIdentifier(), is(ofNullable(prosecutorsDefendant.getDefendantDetails()).map(DefendantDetails::getPncIdentifier).orElse(null)));
        assertThat(prosecutionCaseFileDefendant.getPostingDate(), is(prosecutionSubmissionDetails.getWrittenChargePostingDate()));
        assertThat(prosecutionCaseFileDefendant.getNumPreviousConvictions(), is(ofNullable(prosecutorsDefendant.getDefendantDetails()).map(DefendantDetails::getNumPreviousConvictions).orElse(null)));

        assertAddress(prosecutionCaseFileDefendant.getAddress(), ofNullable(prosecutorsDefendant.getDefendantDetails()).map(DefendantDetails::getAddress).orElse(null));
        assertHearingDetails(prosecutionCaseFileDefendant.getInitialHearing(), prosecutionSubmissionDetails.getHearingDetails());

        assertThat(prosecutionCaseFileDefendant.getCustodyStatus(), is(prosecutorsDefendant.getIndividual().getCustodyStatus()));
        assertThat(prosecutionCaseFileDefendant.getIndividual().getCustodyStatus(), is(prosecutorsDefendant.getIndividual().getCustodyStatus()));
        assertThat(prosecutionCaseFileDefendant.getLanguageRequirement(), is(ofNullable(prosecutorsDefendant.getIndividual()).map(Individual::getLanguageRequirement).orElse(null)));
        assertThat(prosecutionCaseFileDefendant.getSpecificRequirements(), is(ofNullable(prosecutorsDefendant.getIndividual()).map(Individual::getSpecificRequirements).orElse(null)));

    }

    private static void assertAddress(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address pcfAddress, final Address stagingAddress) {
        if(stagingAddress == null) {
            return;
        }

        assertThat(pcfAddress.getAddress1(), is(stagingAddress.getAddress1()));
        assertThat(pcfAddress.getAddress2(), is(stagingAddress.getAddress2()));
        assertThat(pcfAddress.getAddress3(), is(stagingAddress.getAddress3()));
        assertThat(pcfAddress.getAddress4(), is(stagingAddress.getAddress4()));
        assertThat(pcfAddress.getAddress5(), is(stagingAddress.getAddress5()));
        assertThat(pcfAddress.getPostcode(), is(stagingAddress.getPostcode()));
    }

    private static void assertHearingDetails(final InitialHearing pcfHearing, final HearingDetails stagingHearing) {
        assertThat(pcfHearing.getTimeOfHearing(), is(stagingHearing.getTimeOfHearing()));
        assertThat(pcfHearing.getDateOfHearing(), is(stagingHearing.getDateOfHearing().toString()));
        assertThat(pcfHearing.getCourtHearingLocation(), is(stagingHearing.getCourtHearingLocation()));
    }
}