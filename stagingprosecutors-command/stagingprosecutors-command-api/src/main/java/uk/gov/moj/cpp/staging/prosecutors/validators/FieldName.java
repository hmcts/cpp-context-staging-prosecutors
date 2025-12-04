package uk.gov.moj.cpp.staging.prosecutors.validators;

public enum FieldName {
    CASE_URN_NOT_FOUND("urn"),
    EVIDENCE_DETAILS("doesTheProsecutorIntendToServeMoreEvidenceDetails"),
    INVESTIGATION_DETAILS("areThereanypendingEnquiriesorLinesOfInvestigation"),
    INITIAL_DUTY_OF_DISCLOSURE_OF_UNUSED_MATERIALS("HasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWithStateWhenThisWas"),
    SLAVERY_OR_EXPLOITATION_DETAILS("HasDefendantHasBeenAVictimOfSlaveryOrExploitationDetails"),
    EQUIPMENT_REQUIRED("hasDefendantHasBeenAVictimOfSlaveryOrExploitationDetails"),
    UNUSUAL_POINTS_REGARDING_CASE("expectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFactDetails"),
    STANDARD_TRIAL_PREPARATION_TIME_LIMIT("varyAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirectionDetails"),
    EVIDENCE_POST_PTPH("evidencePostPTPH"),
    COTR_EVIDENCE_DETAILS("hasAllEvidenceToBeRelied OnBeenServedDetails"),
    DISCLOSURE_DETAILS("hasAllDisclosureBeenProvidedDetails"),
    DIRECTION_DETAILS("haveOtherDirectionsBeenCompliedWithDetails"),
    WITNESS_DETAILS("haveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails"),
    SUMMONS_DETAILS("haveAnyWitnessSummonsesRequiredBeenReceivedAndServedDetails"),
    SPECIAL_DETAILS("haveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolvedDetails"),
    INTERPRETER_DETAILS("haveInterpretersForWitnessesBeenArrangedDetails"),
    INTERVIEW_DETAILS("haveEditedAbeInterviewsBeenPreparedAndAgreedDetails"),
    ARRANGEMENT_DETAILS("haveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreementDetails"),
    CASE_DETAILS("isTheCaseReadyToProceedWithoutDelayBeforeTheJuryDetails"),
    TIME_ESTIMATE_DETAILS("isTheTimeEstimateCorrectDetails"),
    TRIAL_READINESS_DETAILS("certifyThatTheProsecutionIsTrialReadyDetails"),
    WITNESS_PROSECUTION_WITNESS_REQUIRING_ASSISTANCE_SPECIAL_MEASURE_DETAILS("prosecutionWitnessRequiringAssistanceSpecialMeasuresDetails"),
    WITNESS_INTERPRETER_LANGUAGE_DIALECT("interpreterLanguageDialect"),
    WITNESS_NAME_PTPH("witnessFirstName OR witnessLastName"),
    WITNESS_PROSECUTION_WITNESS_REQUIRING_ASSISTANCE_SPECIAL_MEASURES("prosecutionWitnessRequiringAssistanceSpecialMeasures"),
    AGE_IF_UNDER_18("ageIfUnder18"),
    DOB("dateOfBirth");

    private final String value;

    FieldName(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
