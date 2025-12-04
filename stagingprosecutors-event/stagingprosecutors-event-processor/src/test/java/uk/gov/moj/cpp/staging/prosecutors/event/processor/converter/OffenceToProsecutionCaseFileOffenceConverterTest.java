package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsOffenceList;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

public class OffenceToProsecutionCaseFileOffenceConverterTest {


    private final OffenceToProsecutionCaseFileOffenceConverter converter = new OffenceToProsecutionCaseFileOffenceConverter();

    private static void assertProsecutionCaseFileOffenceListMatchesProsecutionOffenceList(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence> prosecutionCaseFileOffencesList,
                                                                                          final List<Offence> prosecutorsOffenceList) {

        assertThat(prosecutionCaseFileOffencesList, is(notNullValue()));

        assertThat(prosecutionCaseFileOffencesList.size(), is(prosecutorsOffenceList.size()));


        prosecutionCaseFileOffencesList.forEach(
                offences -> {
                    final Offence offence = prosecutorsOffenceList.get(offences.getOffenceSequenceNumber() - 1);
                    assertThat(offences.getAlcoholRelatedOffence().getAlcoholLevelAmount(), is(ofNullable(offence.getOffenceDetails().getAlcoholRelatedOffence()).map(s -> s.getAlcoholOrDrugLevelAmount().intValue()).orElse(null)));
                    assertThat(offences.getAlcoholRelatedOffence().getAlcoholLevelMethod(), is(ofNullable(offence.getOffenceDetails().getAlcoholRelatedOffence()).map(s -> s.getAlcoholOrDrugLevelMethod()).orElse(null)));
                    assertThat(offences.getBackDuty(), is(ofNullable(offence.getOffenceDetails().getBackDuty()).map(BigDecimal::new).orElse(null)));
                    assertThat(offences.getBackDutyDateFrom(), is(offence.getOffenceDetails().getBackDutyDateFrom()));
                    assertThat(offences.getBackDutyDateTo(), is(offence.getOffenceDetails().getBackDutyDateTo()));
                    assertThat(offences.getOffenceCode(), is(offence.getOffenceDetails().getCjsOffenceCode()));
                    assertThat(offences.getOffenceCommittedDate(), is(offence.getOffenceDetails().getOffenceCommittedDate()));
                    assertThat(offences.getOffenceCommittedEndDate(), is(offence.getOffenceDetails().getOffenceCommittedEndDate()));
                    assertThat(offences.getOffenceDateCode(), is(parseInt(offence.getOffenceDetails().getOffenceDateCode().toString())));
                    assertThat(offences.getOffenceLocation(), is(offence.getOffenceDetails().getOffenceLocation()));
                    assertThat(offences.getOffenceSequenceNumber(), is(offence.getOffenceDetails().getOffenceSequenceNo()));
                    assertThat(offences.getOffenceWording(), is(offence.getOffenceDetails().getOffenceWording()));
                    assertThat(offences.getOffenceWordingWelsh(), is(offence.getOffenceDetails().getOffenceWordingWelsh()));
                    assertThat(offences.getAppliedCompensation(), is(ofNullable(offence.getOffenceDetails().getProsecutorCompensation()).map(BigDecimal::new).orElse(null)));
                    assertThat(offences.getChargeDate(), is(offence.getChargeDate()));
                    assertThat(offences.getStatementOfFacts(), is(offence.getStatementOfFacts()));
                    assertThat(offences.getStatementOfFactsWelsh(), is(offences.getStatementOfFactsWelsh()));
                }
        );
    }

    @Test
    public void shouldConvertProsecutionOffenceToProsecutionCaseFileOffence() {

        final List<Offence> prosecutorsOffenceList = prosecutorsOffenceList(3);
        final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence> prosecutionCaseFileOffencesList = converter.convert(prosecutorsOffenceList);

        assertProsecutionCaseFileOffenceListMatchesProsecutionOffenceList(prosecutionCaseFileOffencesList, prosecutorsOffenceList);

    }
}