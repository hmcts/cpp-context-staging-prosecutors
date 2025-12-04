package uk.gov.moj.cpp.staging.prosecutors.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.YesNo.N;

import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServeCotr;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.YesNoNa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class CotrValidatorTest {
    private CotrValidator cotrValidator = new CotrValidator();

    @Test
    public void validateReturnEmptyErrorListWhenThereIsNoWitness() {
        final CpsServeCotr cpsServeCotr = CpsServeCotr.cpsServeCotr()
                .withHasAllEvidenceToBeReliedOnBeenServed(N)
                .withHasAllDisclosureBeenProvided(N)
                .withHaveOtherDirectionsBeenCompliedWith(N)
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend(N)
                .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed(YesNoNa.N)
                .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved(YesNoNa.N)
                .withHaveInterpretersForWitnessesBeenArranged(YesNoNa.N)
                .withHaveEditedAbeInterviewsBeenPreparedAndAgreed(YesNoNa.N)
                .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement(YesNoNa.N)
                .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJury(N)
                .withIsTheTimeEstimateCorrect(N)
                .withCertifyThatTheProsecutionIsTrialReady(N)
                .build();
        final Map<String, List<String>> actualValidations = cotrValidator.validate(cpsServeCotr, new HashMap<>());
        assertThat(actualValidations.size(), is(12));
        assertThat(actualValidations.get(FieldName.COTR_EVIDENCE_DETAILS.getValue()).get(0), is("Evidence Details Missing"));
        assertThat(actualValidations.get(FieldName.DISCLOSURE_DETAILS.getValue()).get(0), is("Disclosure Details Missing"));
        assertThat(actualValidations.get(FieldName.DIRECTION_DETAILS.getValue()).get(0), is("Direction Details Missing"));
        assertThat(actualValidations.get(FieldName.WITNESS_DETAILS.getValue()).get(0), is("Prosecution Witnesses Details Missing"));
        assertThat(actualValidations.get(FieldName.SUMMONS_DETAILS.getValue()).get(0), is("Witnesses Summons Details Missing"));
        assertThat(actualValidations.get(FieldName.SPECIAL_DETAILS.getValue()).get(0), is("Resolution Details Missing"));
        assertThat(actualValidations.get(FieldName.INTERPRETER_DETAILS.getValue()).get(0), is("Interpreter Details Missing"));
        assertThat(actualValidations.get(FieldName.INTERVIEW_DETAILS.getValue()).get(0), is("Interview Details Missing"));
        assertThat(actualValidations.get(FieldName.ARRANGEMENT_DETAILS.getValue()).get(0), is("Agreement and Disagreement Details Missing"));
        assertThat(actualValidations.get(FieldName.CASE_DETAILS.getValue()).get(0), is("Case Details Missing"));
        assertThat(actualValidations.get(FieldName.TIME_ESTIMATE_DETAILS.getValue()).get(0), is("Time Estimate Details Missing"));
        assertThat(actualValidations.get(FieldName.TRIAL_READINESS_DETAILS.getValue()).get(0), is("Trial readiness Details Missing"));
    }


}