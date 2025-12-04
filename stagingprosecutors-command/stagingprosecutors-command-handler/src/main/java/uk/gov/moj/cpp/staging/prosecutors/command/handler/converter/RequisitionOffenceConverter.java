package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;

import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence.offence;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.RequisitionOffence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;

import java.util.ArrayList;
import java.util.List;

public class RequisitionOffenceConverter implements Converter<List<RequisitionOffence>, List<Offence>> {

    @Override
    public List<Offence> convert(final List<RequisitionOffence> requisitionOffenceList) {
        final List<Offence> offenceList = new ArrayList<>();
        requisitionOffenceList.forEach(requisitionOffence -> offenceList.add(offence()
                .withOffenceDetails(requisitionOffence.getOffenceDetails())
                .withStatementOfFacts(requisitionOffence.getStatementOfFacts())
                .withStatementOfFactsWelsh(requisitionOffence.getStatementOfFactsWelsh())
                .withChargeDate(requisitionOffence.getChargeDate())
                .build()));

        return offenceList;
    }
}
