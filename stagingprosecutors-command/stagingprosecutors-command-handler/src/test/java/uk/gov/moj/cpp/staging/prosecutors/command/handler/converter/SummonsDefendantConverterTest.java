package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.staging.prosecutors.command.handler.SummonsDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.test.utils.FileResourceObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class SummonsDefendantConverterTest {

    private SummonsDefendantConverter summonsDefendantConverter = new SummonsDefendantConverter();
    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    @Test
    public void shouldConvertSummonsDefendantToDefendantForIndividual() throws IOException {
        final SummonsDefendant summonsDefendant =
                handlerTestHelper.convertFromFile("json/summonsDefendant.json", SummonsDefendant.class);

        final List<Defendant> defendantList = summonsDefendantConverter.convert(Collections.singletonList(summonsDefendant));

        assertDefendantMatchesSummonDefendant(defendantList.get(0), summonsDefendant);
    }

    @Test
    public void shouldConvertSummonsDefendantToDefendantForOrg() throws IOException {
        final SummonsDefendant summonsDefendant =
                handlerTestHelper.convertFromFile("json/summonsDefendantOrg.json", SummonsDefendant.class);

        final List<Defendant> defendantList = summonsDefendantConverter.convert(Collections.singletonList(summonsDefendant));

        assertDefendantMatchesSummonDefendant(defendantList.get(0), summonsDefendant);
    }

    public static void assertDefendantMatchesSummonDefendant(final Defendant defendant, final SummonsDefendant summonsDefendant) {

        if(defendant.getIndividual() != null) {
            assertThat(defendant.getIndividual().getNameDetails().getForename(), is(summonsDefendant.getIndividual().getNameDetails().getForename()));
            assertThat(defendant.getIndividual().getNameDetails().getSurname(), is(summonsDefendant.getIndividual().getNameDetails().getSurname()));
        } else {
            assertThat(defendant.getOrganisation().getAliasOrganisationNames().get(0), is(summonsDefendant.getOrganisation().getAliasOrganisationNames().get(0)));
            assertThat(defendant.getOrganisation().getCompanyTelephoneNumber(), is(summonsDefendant.getOrganisation().getCompanyTelephoneNumber()));
            assertThat(defendant.getOrganisation().getOrganisationName(), is(summonsDefendant.getOrganisation().getOrganisationName()));
        }

        assertThat(defendant.getDefendantDetails().getProsecutorDefendantId(), is(summonsDefendant.getDefendantDetails().getProsecutorDefendantId()));
        assertThat(defendant.getDefendantDetails().getDocumentationLanguage(), is(summonsDefendant.getDefendantDetails().getDocumentationLanguage()));
        assertThat(defendant.getOffences().get(0).getStatementOfFacts(), is(summonsDefendant.getOffences().get(0).getStatementOfFacts()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getBackDuty(), is(summonsDefendant.getOffences().get(0).getOffenceDetails().getBackDuty()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getBackDutyDateFrom(), is(summonsDefendant.getOffences().get(0).getOffenceDetails().getBackDutyDateFrom()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getBackDutyDateTo(), is(summonsDefendant.getOffences().get(0).getOffenceDetails().getBackDutyDateTo()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getVehicleMake(), is(summonsDefendant.getOffences().get(0).getOffenceDetails().getVehicleMake()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getVehicleRegistrationMark(), is(summonsDefendant.getOffences().get(0).getOffenceDetails().getVehicleRegistrationMark()));

   }

}
