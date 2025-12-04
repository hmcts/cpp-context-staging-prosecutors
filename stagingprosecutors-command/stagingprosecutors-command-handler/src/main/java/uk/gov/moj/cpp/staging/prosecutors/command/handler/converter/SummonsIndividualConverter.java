package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;


import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual.individual;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.SummonsIndividual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual;

public class SummonsIndividualConverter implements Converter<SummonsIndividual, Individual> {

    @Override
    public Individual convert(final SummonsIndividual summonsIndividual) {

        return individual()
                .withNameDetails(summonsIndividual.getNameDetails())
                .withContactDetails(summonsIndividual.getContactDetails())
                .withDateOfBirth(summonsIndividual.getDateOfBirth())
                .withGender(summonsIndividual.getGender())
                .withOccupation(summonsIndividual.getOccupation())
                .withOccupationCode(summonsIndividual.getOccupationCode())
                .withEthnicity(summonsIndividual.getEthnicity())
                .withDriverNumber(summonsIndividual.getDriverNumber())
                .withNationalInsuranceNumber(summonsIndividual.getNationalInsuranceNumber())
                .withParentGuardian(summonsIndividual.getParentGuardian())
                .withAliases(summonsIndividual.getAliases())
                .withLanguageRequirement(summonsIndividual.getLanguageRequirement())
                .withSpecificRequirements(summonsIndividual.getSpecificRequirements())
                .build();
    }
}
