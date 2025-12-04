package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.converter.SjpPersonToProsecutionCaseFileIndividualConverterTest.assertProsecutionCaseFileIndividualMatchesProsecutionPerson;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.converter.SjpProsecutionOffenceToProsecutionCaseFileOffenceConverterTest.assertProsecutionCaseFileOffenceListMatchesProsecutionOffenceList;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.SJPN_POSTING_DATE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsSjpDefendant;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsSjpDefendantOrganisation;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsSjpOffenceList;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsSjpPerson;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpOrganisation;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class SjpDefendantToProsecutionCaseFileDefendantConverterTest {

    @Test
    public void shouldConvertProsecutionDefendantToProsecutionCaseFileDefendant() {

        final Converter<SjpDefendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> converter
                = new SjpDefendantToProsecutionCaseFileDefendantConverter(SJPN_POSTING_DATE);

        final SjpDefendant prosecutorsDefendant = prosecutorsSjpDefendant();


        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant prosecutionCaseFileDefendant = converter.convert(prosecutorsDefendant);

        assertProsecutionCaseFileDefendantMatchesProsecutionDefendant(prosecutionCaseFileDefendant, prosecutorsDefendant);
    }

    @Test
    public void shouldConvertProsecutionDefendantToProsecutionCaseFileDefendantOrganisation_withCorrectWorkTelephoneNumber() {

        final Converter<SjpDefendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> converter
                = new SjpDefendantToProsecutionCaseFileDefendantConverter(SJPN_POSTING_DATE);

        final SjpDefendant prosecutorsDefendant = prosecutorsSjpDefendantOrganisation();


        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant prosecutionCaseFileDefendant = converter.convert(prosecutorsDefendant);


        final SjpOrganisation organisation = prosecutorsDefendant.getOrganisation();
        final ContactDetails contactDetails = organisation.getContactDetails();


        assertEquals(prosecutionCaseFileDefendant.getEmailAddress1(), contactDetails.getPrimaryEmail());
        assertEquals(prosecutionCaseFileDefendant.getTelephoneNumberBusiness(), contactDetails.getWorkTelephoneNumber());
    }


    public static void assertProsecutionCaseFileDefendantMatchesProsecutionDefendant(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant prosecutionCaseFileDefendant,
                                                                                     final SjpDefendant prosecutorsDefendant) {

        assertThat(prosecutionCaseFileDefendant.getAsn(), is(prosecutorsDefendant.getAsn()));
        assertThat(prosecutionCaseFileDefendant.getDocumentationLanguage().name(), is(prosecutorsDefendant.getDocumentationLanguage().name()));
        assertThat(prosecutionCaseFileDefendant.getHearingLanguage().name(), is(prosecutorsDefendant.getHearingLanguage().name()));
        assertThat(prosecutionCaseFileDefendant.getLanguageRequirement(), is(prosecutorsDefendant.getLanguageRequirement()));
        assertThat(prosecutionCaseFileDefendant.getNumPreviousConvictions(), is(prosecutorsDefendant.getNumPreviousConvictions()));
        assertThat(prosecutionCaseFileDefendant.getPostingDate(), is(SJPN_POSTING_DATE));
        assertThat(prosecutionCaseFileDefendant.getSpecificRequirements(), is(prosecutorsDefendant.getSpecificRequirements()));
        assertThat(prosecutionCaseFileDefendant.getAppliedProsecutorCosts(), is(new BigDecimal(prosecutorsDefendant.getProsecutorCosts())));

        assertProsecutionCaseFileIndividualMatchesProsecutionPerson(prosecutionCaseFileDefendant.getIndividual(),
                prosecutorsSjpPerson());

        assertProsecutionCaseFileOffenceListMatchesProsecutionOffenceList(prosecutionCaseFileDefendant.getOffences(),
                prosecutorsSjpOffenceList(prosecutorsDefendant.getOffences().size()));

    }
}