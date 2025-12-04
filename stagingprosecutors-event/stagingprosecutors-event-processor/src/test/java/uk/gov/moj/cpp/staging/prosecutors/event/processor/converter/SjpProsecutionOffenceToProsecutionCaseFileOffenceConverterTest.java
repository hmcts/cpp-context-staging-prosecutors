package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsSjpOffenceList;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpOffence;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

public class SjpProsecutionOffenceToProsecutionCaseFileOffenceConverterTest {


    private final SjpProsecutionOffenceToProsecutionCaseFileOffenceConverter converter = new SjpProsecutionOffenceToProsecutionCaseFileOffenceConverter();

    public static void assertProsecutionCaseFileOffenceListMatchesProsecutionOffenceList(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence> prosecutionCaseFileOffencesList,
                                                                                         final List<SjpOffence> prosecutorsOffenceList) {

        assertThat(prosecutionCaseFileOffencesList, is(notNullValue()));

        assertThat(prosecutionCaseFileOffencesList.size(), is(prosecutorsOffenceList.size()));


        prosecutionCaseFileOffencesList.forEach(
                offences -> {
                    final SjpOffence offence = prosecutorsOffenceList.get(offences.getOffenceSequenceNumber() - 1);
                    assertThat(offences.getBackDuty(), is(ofNullable(offence.getBackDuty()).map(BigDecimal::new).orElse(null)));
                    assertThat(offences.getBackDutyDateFrom(), is(offence.getBackDutyDateFrom()));
                    assertThat(offences.getBackDutyDateTo(), is(offence.getBackDutyDateTo()));
                    assertThat(offences.getChargeDate(), is(offence.getChargeDate()));
                    assertThat(offences.getAppliedCompensation(), is(ofNullable(offence.getProsecutorCompensation()).map(BigDecimal::new).orElse(null)));
                    assertThat(offences.getOffenceCode(), is(offence.getCjsOffenceCode()));
                    assertThat(offences.getOffenceCommittedEndDate(), is(offence.getOffenceCommittedEndDate()));
                    assertThat(offences.getOffenceCommittedDate(), is(offence.getOffenceCommittedDate()));
                    assertThat(offences.getOffenceDateCode(), is(offence.getOffenceDateCode()));
                    assertThat(offences.getOffenceLocation(), is(offence.getOffenceLocation()));
                    assertThat(offences.getOffenceSequenceNumber(), is(offence.getOffenceSequenceNo()));
                    assertThat(offences.getOffenceWording(), is(offence.getOffenceWording()));
                    assertThat(offences.getOffenceWordingWelsh(), is(offence.getOffenceWordingWelsh()));
                    assertThat(offences.getStatementOfFacts(), is(offence.getStatementOfFacts()));
                    assertThat(offences.getStatementOfFactsWelsh(), is(offences.getStatementOfFactsWelsh()));
                    assertThat(offences.getProsecutorOfferAOCP(), is(offences.getProsecutorOfferAOCP()));

                }
        );
    }

    @Test
    public void shouldConvertProsecutionOffenceToProsecutionCaseFileOffence() {

        final List<SjpOffence> prosecutorsOffenceList = prosecutorsSjpOffenceList(3);
        final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence> prosecutionCaseFileOffencesList = converter.convert(prosecutorsOffenceList);

        assertProsecutionCaseFileOffenceListMatchesProsecutionOffenceList(prosecutionCaseFileOffencesList, prosecutorsOffenceList);

    }
}