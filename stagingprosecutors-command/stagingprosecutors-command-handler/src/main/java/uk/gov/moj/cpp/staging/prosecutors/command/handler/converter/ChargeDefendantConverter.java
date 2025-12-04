package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;


import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant.defendant;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.ChargeDefendant;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.ChargeIndividual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;

import java.util.ArrayList;
import java.util.List;

public class ChargeDefendantConverter implements Converter<List<ChargeDefendant>, List<Defendant>> {

    private final ChargeIndividualConverter chargeIndividualConverter = new ChargeIndividualConverter();
    private final ChargeOffenceConverter chargeOffenceConverter = new ChargeOffenceConverter();

    @Override
    public List<Defendant> convert(final List<ChargeDefendant> chargeDefendantList) {
        final List<Defendant> defendantList = new ArrayList<>();

        chargeDefendantList.forEach(chargeDefendant -> {
            final Defendant.Builder defendantBuilder = defendant()
                    .withDefendantDetails(chargeDefendant.getDefendantDetails())
                    .withOffences(chargeOffenceConverter.convert(chargeDefendant.getOffences()));

            if (chargeDefendant.getIndividual() != null) {
                final ChargeIndividual chargeIndividual = chargeDefendant.getIndividual();
                defendantBuilder.withIndividual(chargeIndividualConverter.convert(chargeIndividual));
            } else {
                defendantBuilder.withOrganisation(chargeDefendant.getOrganisation());
            }

            defendantList.add(defendantBuilder.build());
        });

        return defendantList;
    }

}
