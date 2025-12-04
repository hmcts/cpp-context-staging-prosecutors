package uk.gov.moj.cpp.staging.prosecutors.validators;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServePet;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Witnesses;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"squid:MethodCyclomaticComplexity","squid:S3776", "squid:S3655"})
public class PetValidator {

    private static final String YES = "Y";
    private static final String NO = "N";

    public Map<String, List<String>> validate(final CpsServePet cpsServePet, final Map<String, List<String>> validationErrors) {

        if (valueYesButDetailsNotPresent(cpsServePet.getDoesTheProsecutorIntendToServeMoreEvidence().toString(), ofNullable(cpsServePet.getDoesTheProsecutorIntendToServeMoreEvidenceDetails()))) {
            addError(validationErrors, FieldName.EVIDENCE_DETAILS.getValue(), "Evidence Details Missing");
        }

        if (valueYesButDetailsNotPresent(cpsServePet.getAreThereanypendingEnquiriesorLinesOfInvestigation().toString(), ofNullable(cpsServePet.getAreThereanypendingEnquiriesorLinesOfInvestigationDetais()))) {
            addError(validationErrors, FieldName.INVESTIGATION_DETAILS.getValue(), "Pending Enquiries or Lines Of Investigation details Missing");
        }

        if(valueYesButDetailsNotPresent(cpsServePet.getHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith().toString(),ofNullable(cpsServePet.getHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWithStateWhenThisWas()))) {
            addError(validationErrors,FieldName.INITIAL_DUTY_OF_DISCLOSURE_OF_UNUSED_MATERIALS.getValue(), "State when unused prosecution material was disclosed");
        }

        if(valueYesButDetailsNotPresent(cpsServePet.getHasDefendantHasBeenAVictimOfSlaveryOrExploitation().toString(),ofNullable(cpsServePet.getHasDefendantHasBeenAVictimOfSlaveryOrExploitationDetails()))){
            addError(validationErrors,FieldName.SLAVERY_OR_EXPLOITATION_DETAILS.getValue(), "Details suggesting that the defendant has been a victim of slavery or exploitation");
        }

        if(valueYesButDetailsNotPresent(cpsServePet.getWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoom().toString(), ofNullable(cpsServePet.getWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoomDetails()))){
            addError(validationErrors,FieldName.EQUIPMENT_REQUIRED.getValue(), "Details of the equipment required in the trial room");
        }

        if(valueYesButDetailsNotPresent(cpsServePet.getExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFact().toString(), ofNullable(cpsServePet.getExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFactDetails()))) {
            addError(validationErrors, FieldName.UNUSUAL_POINTS_REGARDING_CASE.getValue(), "Unusual points regarding the case is missing");
        }

        if(valueYesButDetailsNotPresent(cpsServePet.getVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirection().toString(),ofNullable(cpsServePet.getVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirectionDetails()))){
            addError(validationErrors,FieldName.STANDARD_TRIAL_PREPARATION_TIME_LIMIT.getValue(), "Standard trial preparation time limit or direction details.");
        }

        if (isNotEmpty(cpsServePet.getWitnesses())) {
            final List<Witnesses> witnessesList = cpsServePet.getWitnesses();
            for (final Witnesses witnesses : witnessesList) {
                if (valueYesButDetailsNotPresent(witnesses.getProsecutionWitnessRequiringAssistanceSpecialMeasures(), ofNullable(witnesses.getProsecutionWitnessRequiringAssistanceSpecialMeasuresDetails()))) {
                    addError(validationErrors, FieldName.WITNESS_PROSECUTION_WITNESS_REQUIRING_ASSISTANCE_SPECIAL_MEASURE_DETAILS.getValue(), "Prosecution witness requiring assistance special measure details missing.");
                }
                else if (!isValid(witnesses.getProsecutionWitnessRequiringAssistanceSpecialMeasures())){
                    addError(validationErrors, FieldName.WITNESS_PROSECUTION_WITNESS_REQUIRING_ASSISTANCE_SPECIAL_MEASURES.getValue(), "Invalid value.");
                }

                if (witnesses.getInterpreterRequired() && !ofNullable(witnesses.getInterpreterLanguageDialect()).isPresent()) {
                    addError(validationErrors, FieldName.WITNESS_INTERPRETER_LANGUAGE_DIALECT.getValue(), "Witness interpreter language dialect missing.");
                }

                if(ofNullable(witnesses.getAgeIfUnder18()).isPresent() && ofNullable(witnesses.getAgeIfUnder18()).get() > 18){
                    addError(validationErrors, FieldName.AGE_IF_UNDER_18.getValue(), "Value should be less than 18.");
                }

                if(ofNullable(witnesses.getDateOfBirth()).isPresent() && isDateFuture(witnesses.getDateOfBirth())){
                    addError(validationErrors, FieldName.DOB.getValue(), "Date cannot be in the future");
                }
            }
        }

        return validationErrors;
    }

    private boolean valueYesButDetailsNotPresent(final String field, final Optional<String> fieldDetails) {
        return nonNull(field) && YES.equals(field) && !fieldDetails.isPresent();
    }

    private boolean isValid(final String field) {
        return nonNull(field) && (YES.equals(field) || NO.equals(field));
    }

    private void addError(Map<String, List<String>> errors, String field, String error) {
        errors.putIfAbsent(field, new ArrayList<>());
        errors.get(field).add(error);
    }

    private boolean isDateFuture(final String date) {
        final LocalDate localDate = LocalDate.now(ZoneId.systemDefault());

        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        final LocalDate inputDate = LocalDate.parse(date, dtf);

        return inputDate.isAfter(localDate);
    }
}
