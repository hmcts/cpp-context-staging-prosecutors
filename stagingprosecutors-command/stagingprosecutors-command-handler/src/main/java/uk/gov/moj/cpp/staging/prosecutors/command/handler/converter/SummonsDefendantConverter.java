package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;


import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant.defendant;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.SummonsDefendant;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.SummonsIndividual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;

import java.util.ArrayList;
import java.util.List;

public class SummonsDefendantConverter implements Converter< List<SummonsDefendant>, List<Defendant> > {

    private final SummonsOffenceConverter summonsOffenceConverter = new SummonsOffenceConverter();
    private final SummonsIndividualConverter summonsIndividualConverter = new SummonsIndividualConverter();

    @Override
    public List<Defendant> convert(final List<SummonsDefendant> summonsDefendantList) {
        final List<Defendant> defendantList = new ArrayList<>();

        summonsDefendantList.forEach(summonsDefendant -> {
            final Defendant.Builder defendantBuilder = defendant()
                    .withDefendantDetails(summonsDefendant.getDefendantDetails())
                    .withOffences(summonsOffenceConverter.convert(summonsDefendant.getOffences()));

            if (summonsDefendant.getIndividual() != null) {
                final SummonsIndividual summonsIndividual = summonsDefendant.getIndividual();
                defendantBuilder.withIndividual(summonsIndividualConverter.convert(summonsIndividual));
            } else {
                defendantBuilder.withOrganisation(summonsDefendant.getOrganisation());
            }

            defendantList.add(defendantBuilder.build());
        });

        return defendantList;

    }

}
