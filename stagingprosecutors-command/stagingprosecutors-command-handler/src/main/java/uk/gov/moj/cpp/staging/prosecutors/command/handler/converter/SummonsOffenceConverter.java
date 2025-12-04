package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;

import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence.offence;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.SummonsOffence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;

import java.util.ArrayList;
import java.util.List;

public class SummonsOffenceConverter implements Converter<List<SummonsOffence>, List<Offence>> {

    @Override
    public List<Offence> convert(final List<SummonsOffence> summonsOffenceList) {
        final List<Offence> offenceList = new ArrayList<>();
        summonsOffenceList.forEach(summonsOffence -> offenceList.add(offence()
                .withOffenceDetails(summonsOffence.getOffenceDetails())
                .withStatementOfFacts(summonsOffence.getStatementOfFacts())
                .withStatementOfFactsWelsh(summonsOffence.getStatementOfFactsWelsh())
                .build()));

        return offenceList;
    }
}
