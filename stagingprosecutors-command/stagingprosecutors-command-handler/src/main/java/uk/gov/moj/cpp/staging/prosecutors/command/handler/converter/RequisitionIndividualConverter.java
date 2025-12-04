package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;


import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual.individual;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.RequisitionIndividual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual;

public class RequisitionIndividualConverter implements Converter<RequisitionIndividual, Individual> {

    @Override
    public Individual convert(final RequisitionIndividual requisitionIndividual) {

        return individual()
                .withNameDetails(requisitionIndividual.getNameDetails())
                .withContactDetails(requisitionIndividual.getContactDetails())
                .withDateOfBirth(requisitionIndividual.getDateOfBirth())
                .withGender(requisitionIndividual.getGender())
                .withOccupation(requisitionIndividual.getOccupation())
                .withOccupationCode(requisitionIndividual.getOccupationCode())
                .withEthnicity(requisitionIndividual.getEthnicity())
                .withDriverNumber(requisitionIndividual.getDriverNumber())
                .withNationalInsuranceNumber(requisitionIndividual.getNationalInsuranceNumber())
                .withParentGuardian(requisitionIndividual.getParentGuardian())
                .withAliases(requisitionIndividual.getAliases())
                .withSpecificRequirements(requisitionIndividual.getSpecificRequirements())
                .withLanguageRequirement(requisitionIndividual.getLanguageRequirement())
                .build();
    }
}
