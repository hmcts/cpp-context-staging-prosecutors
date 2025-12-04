package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.staging.prosecutors.command.handler.ChargeDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.test.utils.FileResourceObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ChargeDefendantConverterTest {

    private final ChargeDefendantConverter chargeDefendantConverter = new ChargeDefendantConverter();
    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    @Test
    public void shouldConvertChargeDefendantToDefendantForIndividual() throws IOException {
        final ChargeDefendant chargeDefendant =
                handlerTestHelper.convertFromFile("json/chargeDefendant.json", ChargeDefendant.class);

        final List<Defendant> defendantList = chargeDefendantConverter.convert(Collections.singletonList(chargeDefendant));

        assertDefendantMatchesChargeDefendant(defendantList.get(0), chargeDefendant);
    }

    @Test
    public void shouldConvertChargeDefendantToDefendantForOrg() throws IOException {
        final ChargeDefendant chargeDefendant =
                handlerTestHelper.convertFromFile("json/chargeDefendantOrg.json", ChargeDefendant.class);

        final List<Defendant> defendantList = chargeDefendantConverter.convert(Collections.singletonList(chargeDefendant));

        assertDefendantMatchesChargeDefendant(defendantList.get(0), chargeDefendant);
    }

    public static void assertDefendantMatchesChargeDefendant(final Defendant defendant, final ChargeDefendant chargeDefendant) {

        if(defendant.getIndividual() != null ) {
            assertThat(defendant.getIndividual().getNameDetails().getForename(), is(chargeDefendant.getIndividual().getNameDetails().getForename()));
            assertThat(defendant.getIndividual().getNameDetails().getSurname(), is(chargeDefendant.getIndividual().getNameDetails().getSurname()));
            assertThat(defendant.getIndividual().getCustodyStatus(), is(chargeDefendant.getIndividual().getCustodyStatus()));
        } else {
            assertThat(defendant.getOrganisation().getAliasOrganisationNames().get(0), is(chargeDefendant.getOrganisation().getAliasOrganisationNames().get(0)));
            assertThat(defendant.getOrganisation().getCompanyTelephoneNumber(), is(chargeDefendant.getOrganisation().getCompanyTelephoneNumber()));
            assertThat(defendant.getOrganisation().getOrganisationName(), is(chargeDefendant.getOrganisation().getOrganisationName()));
        }

        assertThat(defendant.getDefendantDetails().getProsecutorDefendantId(), is(chargeDefendant.getDefendantDetails().getProsecutorDefendantId()));
        assertThat(defendant.getDefendantDetails().getDocumentationLanguage(), is(chargeDefendant.getDefendantDetails().getDocumentationLanguage()));
        assertThat(defendant.getOffences().get(0).getArrestDate(), is(chargeDefendant.getOffences().get(0).getArrestDate()));
        assertThat(defendant.getOffences().get(0).getChargeDate(), is(chargeDefendant.getOffences().get(0).getChargeDate()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getBackDuty(), is(chargeDefendant.getOffences().get(0).getOffenceDetails().getBackDuty()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getBackDutyDateFrom(), is(chargeDefendant.getOffences().get(0).getOffenceDetails().getBackDutyDateFrom()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getBackDutyDateTo(), is(chargeDefendant.getOffences().get(0).getOffenceDetails().getBackDutyDateTo()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getVehicleMake(), is(chargeDefendant.getOffences().get(0).getOffenceDetails().getVehicleMake()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getVehicleRegistrationMark(), is(chargeDefendant.getOffences().get(0).getOffenceDetails().getVehicleRegistrationMark()));
    }
}
