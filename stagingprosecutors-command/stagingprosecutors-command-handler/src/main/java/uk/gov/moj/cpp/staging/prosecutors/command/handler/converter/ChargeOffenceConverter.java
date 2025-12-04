package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;


import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence.offence;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.ChargeOffence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;

import java.util.ArrayList;
import java.util.List;

public class ChargeOffenceConverter implements Converter<List<ChargeOffence>, List<Offence>> {

    @Override
    public List<Offence> convert(final List<ChargeOffence> chargeOffenceList) {
        final List<Offence> offenceList = new ArrayList<>();

        chargeOffenceList.forEach(chargeOffence -> offenceList.add(offence()
                .withOffenceDetails(chargeOffence.getOffenceDetails())
                .withArrestDate(chargeOffence.getArrestDate())
                .withChargeDate(chargeOffence.getChargeDate())
                .build()));

        return offenceList;
    }

}
