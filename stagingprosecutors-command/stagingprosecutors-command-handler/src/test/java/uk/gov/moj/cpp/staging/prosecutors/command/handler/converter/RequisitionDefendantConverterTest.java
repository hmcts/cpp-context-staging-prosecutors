package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.staging.prosecutors.command.handler.RequisitionDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.test.utils.FileResourceObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class RequisitionDefendantConverterTest {

    private final RequisitionDefendantConverter requisitionDefendantConverter = new RequisitionDefendantConverter();
    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    @Test
    public void shouldConvertRequisitionDefendantToDefendantForIndividual() throws IOException {
        final RequisitionDefendant requisitionDefendant =
                handlerTestHelper.convertFromFile("json/requisitionDefendant.json", RequisitionDefendant.class);

        final List<Defendant> defendantList = requisitionDefendantConverter.convert(Collections.singletonList(requisitionDefendant));

        assertDefendantMatchesRequisitionDefendant(defendantList.get(0), requisitionDefendant);
    }

    @Test
    public void shouldConvertRequisitionDefendantToDefendantForOrg() throws IOException {
        final RequisitionDefendant requisitionDefendant =
                handlerTestHelper.convertFromFile("json/requisitionDefendantOrg.json", RequisitionDefendant.class);

        final List<Defendant> defendantList = requisitionDefendantConverter.convert(Collections.singletonList(requisitionDefendant));

        assertDefendantMatchesRequisitionDefendant(defendantList.get(0), requisitionDefendant);
    }

    public static void assertDefendantMatchesRequisitionDefendant(final Defendant defendant, final RequisitionDefendant requisitionDefendant) {

        if(defendant.getIndividual() != null ) {
            assertThat(defendant.getIndividual().getNameDetails().getForename(), is(requisitionDefendant.getIndividual().getNameDetails().getForename()));
            assertThat(defendant.getIndividual().getNameDetails().getSurname(), is(requisitionDefendant.getIndividual().getNameDetails().getSurname()));
        } else {
            assertThat(defendant.getOrganisation().getAliasOrganisationNames().get(0), is(requisitionDefendant.getOrganisation().getAliasOrganisationNames().get(0)));
            assertThat(defendant.getOrganisation().getCompanyTelephoneNumber(), is(requisitionDefendant.getOrganisation().getCompanyTelephoneNumber()));
            assertThat(defendant.getOrganisation().getOrganisationName(), is(requisitionDefendant.getOrganisation().getOrganisationName()));
        }

        assertThat(defendant.getDefendantDetails().getProsecutorDefendantId(), is(requisitionDefendant.getDefendantDetails().getProsecutorDefendantId()));
        assertThat(defendant.getDefendantDetails().getDocumentationLanguage(), is(requisitionDefendant.getDefendantDetails().getDocumentationLanguage()));
        assertThat(defendant.getOffences().get(0).getChargeDate(), is(requisitionDefendant.getOffences().get(0).getChargeDate()));
        assertThat(defendant.getOffences().get(0).getStatementOfFacts(), is(requisitionDefendant.getOffences().get(0).getStatementOfFacts()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getBackDuty(), is(requisitionDefendant.getOffences().get(0).getOffenceDetails().getBackDuty()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getBackDutyDateFrom(), is(requisitionDefendant.getOffences().get(0).getOffenceDetails().getBackDutyDateFrom()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getBackDutyDateTo(), is(requisitionDefendant.getOffences().get(0).getOffenceDetails().getBackDutyDateTo()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getVehicleMake(), is(requisitionDefendant.getOffences().get(0).getOffenceDetails().getVehicleMake()));
        assertThat(defendant.getOffences().get(0).getOffenceDetails().getVehicleRegistrationMark(), is(requisitionDefendant.getOffences().get(0).getOffenceDetails().getVehicleRegistrationMark()));
    }
}
