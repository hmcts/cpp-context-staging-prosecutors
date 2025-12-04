package uk.gov.moj.cpp.staging.prosecutors.validators;

import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServeCotr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

@SuppressWarnings({"squid:UnusedPrivateMethod", "squid:S1172"})
public class CotrValidator {

    private static final String NO = "N";

    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    public Map<String, List<String>> validate(final CpsServeCotr cpsServeCotr, final Map<String, List<String>> validationErrors) {

        if(valueNoButDetailsNotPresent(cpsServeCotr.getHasAllEvidenceToBeReliedOnBeenServed().toString(), ofNullable(cpsServeCotr.getHasAllEvidenceToBeReliedOnBeenServedDetails()))) {
            addError(validationErrors, FieldName.COTR_EVIDENCE_DETAILS.getValue(), "Evidence Details Missing");
        }

        if(valueNoButDetailsNotPresent(cpsServeCotr.getHasAllDisclosureBeenProvided().toString(), ofNullable(cpsServeCotr.getHasAllDisclosureBeenProvidedDetails()))) {
            addError(validationErrors, FieldName.DISCLOSURE_DETAILS.getValue(), "Disclosure Details Missing");
        }

        if(valueNoButDetailsNotPresent(cpsServeCotr.getHaveOtherDirectionsBeenCompliedWith().toString(), ofNullable(cpsServeCotr.getHaveOtherDirectionsBeenCompliedWithDetails()))) {
            addError(validationErrors, FieldName.DIRECTION_DETAILS.getValue(), "Direction Details Missing");
        }

        if(valueNoButDetailsNotPresent(cpsServeCotr.getHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend().toString(), ofNullable(cpsServeCotr.getHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails()))) {
            addError(validationErrors, FieldName.WITNESS_DETAILS.getValue(), "Prosecution Witnesses Details Missing");
        }

        if(valueNoButDetailsNotPresent(cpsServeCotr.getHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed().toString(), ofNullable(cpsServeCotr.getHaveAnyWitnessSummonsesRequiredBeenReceivedAndServedDetails()))) {
            addError(validationErrors, FieldName.SUMMONS_DETAILS.getValue(), "Witnesses Summons Details Missing");
        }

        if(valueNoButDetailsNotPresent(cpsServeCotr.getHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved().toString(), ofNullable(cpsServeCotr.getHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolvedDetails()))) {
            addError(validationErrors, FieldName.SPECIAL_DETAILS.getValue(), "Resolution Details Missing");
        }

        if(valueNoButDetailsNotPresent(cpsServeCotr.getHaveInterpretersForWitnessesBeenArranged().toString(), ofNullable(cpsServeCotr.getHaveInterpretersForWitnessesBeenArrangedDetails()))) {
            addError(validationErrors, FieldName.INTERPRETER_DETAILS.getValue(), "Interpreter Details Missing");
        }

        if(valueNoButDetailsNotPresent(cpsServeCotr.getHaveEditedAbeInterviewsBeenPreparedAndAgreed().toString(), ofNullable(cpsServeCotr.getHaveEditedAbeInterviewsBeenPreparedAndAgreedDetails()))) {
            addError(validationErrors, FieldName.INTERVIEW_DETAILS.getValue(), "Interview Details Missing");
        }

        if(valueNoButDetailsNotPresent(cpsServeCotr.getHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement().toString(), ofNullable(cpsServeCotr.getHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreementDetails()))) {
            addError(validationErrors, FieldName.ARRANGEMENT_DETAILS.getValue(), "Agreement and Disagreement Details Missing");
        }

        if(valueNoButDetailsNotPresent(cpsServeCotr.getIsTheCaseReadyToProceedWithoutDelayBeforeTheJury().toString(), ofNullable(cpsServeCotr.getIsTheCaseReadyToProceedWithoutDelayBeforeTheJuryDetails()))) {
            addError(validationErrors, FieldName.CASE_DETAILS.getValue(), "Case Details Missing");
        }

        if(valueNoButDetailsNotPresent(cpsServeCotr.getIsTheTimeEstimateCorrect().toString(), ofNullable(cpsServeCotr.getIsTheTimeEstimateCorrectDetails()))) {
            addError(validationErrors, FieldName.TIME_ESTIMATE_DETAILS.getValue(), "Time Estimate Details Missing");
        }

        if(valueNoButDetailsNotPresent(cpsServeCotr.getCertifyThatTheProsecutionIsTrialReady().toString(), ofNullable(cpsServeCotr.getCertifyThatTheProsecutionIsTrialReadyDetails()))) {
            addError(validationErrors, FieldName.TRIAL_READINESS_DETAILS.getValue(), "Trial readiness Details Missing");
        }

        return validationErrors;
    }

    private boolean valueNoButDetailsNotPresent(final String field, final Optional<String> fieldDetails) {
        return nonNull(field) && NO.equalsIgnoreCase(field) && !fieldDetails.isPresent();
    }

    private void addError(final Map<String, List<String>> errors, final String field, final String error) {
        errors.putIfAbsent(field, new ArrayList<>());
        errors.get(field).add(error);
    }
}
