package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;

import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual.individual;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.ChargeIndividual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual;

public class ChargeIndividualConverter implements Converter<ChargeIndividual, Individual> {

    @Override
    public Individual convert(final ChargeIndividual chargeIndividual) {
        return individual()
                .withNameDetails(chargeIndividual.getNameDetails())
                .withContactDetails(chargeIndividual.getContactDetails())
                .withDateOfBirth(chargeIndividual.getDateOfBirth())
                .withGender(chargeIndividual.getGender())
                .withOccupation(chargeIndividual.getOccupation())
                .withOccupationCode(chargeIndividual.getOccupationCode())
                .withEthnicity(chargeIndividual.getEthnicity())
                .withDriverNumber(chargeIndividual.getDriverNumber())
                .withNationalInsuranceNumber(chargeIndividual.getNationalInsuranceNumber())
                .withParentGuardian(chargeIndividual.getParentGuardian())
                .withAliases(chargeIndividual.getAliases())
                .withObservedEthnicity(chargeIndividual.getObservedEthnicity())
                .withLanguageRequirement(chargeIndividual.getLanguageRequirement())
                .withSpecificRequirements(chargeIndividual.getSpecificRequirements())
                .withCustodyStatus(chargeIndividual.getCustodyStatus())
                .withBailConditions(chargeIndividual.getBailConditions())
                .build();
    }
}
