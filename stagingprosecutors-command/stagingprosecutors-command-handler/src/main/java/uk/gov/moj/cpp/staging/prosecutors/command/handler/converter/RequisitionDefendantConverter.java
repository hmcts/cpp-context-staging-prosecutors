package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;

import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant.defendant;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.RequisitionDefendant;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.RequisitionIndividual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;

import java.util.ArrayList;
import java.util.List;

public class RequisitionDefendantConverter implements Converter< List<RequisitionDefendant>, List<Defendant> > {

    private final RequisitionOffenceConverter requisitionOffenceConverter = new RequisitionOffenceConverter();
    private final RequisitionIndividualConverter requisitionIndividualConverter = new RequisitionIndividualConverter();

    @Override
    public List<Defendant> convert(final List<RequisitionDefendant> requisitionDefendantList) {
        final List<Defendant> defendantList = new ArrayList<>();

        requisitionDefendantList.forEach(requisitionDefendant -> {
            final Defendant.Builder defendantBuilder = defendant()
                    .withDefendantDetails(requisitionDefendant.getDefendantDetails())
                    .withOffences(requisitionOffenceConverter.convert(requisitionDefendant.getOffences()));

            if (requisitionDefendant.getIndividual() != null) {
                final RequisitionIndividual requisitionIndividual = requisitionDefendant.getIndividual();
                defendantBuilder.withIndividual(requisitionIndividualConverter.convert(requisitionIndividual));
            } else {
                defendantBuilder.withOrganisation(requisitionDefendant.getOrganisation());
            }

            defendantList.add(defendantBuilder.build());
        });

        return defendantList;

    }
}
