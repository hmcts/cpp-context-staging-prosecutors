package uk.gov.moj.cpp.staging.prosecutors.validators;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.staging.prosecutors.command.api.AreThereanypendingEnquiriesorLinesOfInvestigation;
import uk.gov.moj.cpp.staging.prosecutors.command.api.CourtToArrangeADiscussionOfGroundRulesForQuestioning;
import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServePet;
import uk.gov.moj.cpp.staging.prosecutors.command.api.DoesTheProsecutorIntendToServeMoreEvidence;
import uk.gov.moj.cpp.staging.prosecutors.command.api.ExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFact;
import uk.gov.moj.cpp.staging.prosecutors.command.api.HasDefendantHasBeenAVictimOfSlaveryOrExploitation;
import uk.gov.moj.cpp.staging.prosecutors.command.api.HasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith;
import uk.gov.moj.cpp.staging.prosecutors.command.api.VaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirection;
import uk.gov.moj.cpp.staging.prosecutors.command.api.WillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoom;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsProsecutionCaseSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantOffencesSubjects;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionWillRelyOn;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Witnesses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class PetValidatorTest {

    private PetValidator petValidator = new PetValidator();

    @Test
    public void validateDoesTheProsecutorIntendToServeMoreEvidence(){
        CpsServePet.Builder builder = createCpsServePetPayload();
        CpsServePet cpsServePet = builder.withDoesTheProsecutorIntendToServeMoreEvidence(DoesTheProsecutorIntendToServeMoreEvidence.Y).build();
        final Map<String, List<String>> actualValidations = petValidator.validate(cpsServePet, new HashMap<>());

        final String errorMsg = "Evidence Details Missing";
        final List<Pair<String, String>> errors = newArrayList(
                Pair.of(FieldName.EVIDENCE_DETAILS.getValue(), errorMsg));

        thenValidationFailsWith(actualValidations, errors);
    }

    @Test
    public void validateAreThereAnyPendingEnquiriesOrLinesOfInvestigation(){
        CpsServePet.Builder builder = createCpsServePetPayload();
        CpsServePet cpsServePet = builder.withAreThereanypendingEnquiriesorLinesOfInvestigation(AreThereanypendingEnquiriesorLinesOfInvestigation.Y).build();
        final Map<String, List<String>> actualValidations = petValidator.validate(cpsServePet, new HashMap<>());

        final String errorMsg = "Pending Enquiries or Lines Of Investigation details Missing";
        final List<Pair<String, String>> errors = newArrayList(
                Pair.of(FieldName.INVESTIGATION_DETAILS.getValue(), errorMsg));

        thenValidationFailsWith(actualValidations, errors);
    }

    @Test
    public void validateHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith(){
        CpsServePet.Builder builder = createCpsServePetPayload();
        CpsServePet cpsServePet = builder.withHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith(HasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith.Y).build();
        final Map<String, List<String>> actualValidations = petValidator.validate(cpsServePet, new HashMap<>());

        final String errorMsg = "State when unused prosecution material was disclosed";
        final List<Pair<String, String>> errors = newArrayList(
                Pair.of(FieldName.INITIAL_DUTY_OF_DISCLOSURE_OF_UNUSED_MATERIALS.getValue(), errorMsg));

        thenValidationFailsWith(actualValidations, errors);
    }

    @Test
    public void validateHasDefendantHasBeenAVictimOfSlaveryOrExploitation(){
        CpsServePet.Builder builder = createCpsServePetPayload();
        CpsServePet cpsServePet = builder.withHasDefendantHasBeenAVictimOfSlaveryOrExploitation(HasDefendantHasBeenAVictimOfSlaveryOrExploitation.Y).build();
        final Map<String, List<String>> actualValidations = petValidator.validate(cpsServePet, new HashMap<>());

        final String errorMsg = "Details suggesting that the defendant has been a victim of slavery or exploitation";
        final List<Pair<String, String>> errors = newArrayList(
                Pair.of(FieldName.SLAVERY_OR_EXPLOITATION_DETAILS.getValue(), errorMsg));

        thenValidationFailsWith(actualValidations, errors);
    }

    @Test
    public void validateWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoom(){
        CpsServePet.Builder builder = createCpsServePetPayload();
        CpsServePet cpsServePet = builder.withWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoom(WillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoom.Y).build();
        final Map<String, List<String>> actualValidations = petValidator.validate(cpsServePet, new HashMap<>());

        final String errorMsg = "Details of the equipment required in the trial room";
        final List<Pair<String, String>> errors = newArrayList(
                Pair.of(FieldName.EQUIPMENT_REQUIRED.getValue(), errorMsg));

        thenValidationFailsWith(actualValidations, errors);
    }

    @Test
    public void validateExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFact(){
        CpsServePet.Builder builder = createCpsServePetPayload();
        CpsServePet cpsServePet = builder.withExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFact(ExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFact.Y).build();
        final Map<String, List<String>> actualValidations = petValidator.validate(cpsServePet, new HashMap<>());

        final String errorMsg = "Unusual points regarding the case is missing";
        final List<Pair<String, String>> errors = newArrayList(
                Pair.of(FieldName.UNUSUAL_POINTS_REGARDING_CASE.getValue(), errorMsg));

        thenValidationFailsWith(actualValidations, errors);
    }

    @Test
    public void validateVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirection(){
        CpsServePet.Builder builder = createCpsServePetPayload();
        CpsServePet cpsServePet = builder.withVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirection(VaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirection.Y).build();
        final Map<String, List<String>> actualValidations = petValidator.validate(cpsServePet, new HashMap<>());

        final String errorMsg = "Standard trial preparation time limit or direction details.";
        final List<Pair<String, String>> errors = newArrayList(
                Pair.of(FieldName.STANDARD_TRIAL_PREPARATION_TIME_LIMIT.getValue(), errorMsg));

        thenValidationFailsWith(actualValidations, errors);
    }

    @Test
    public void validateProsecutionWitnessRequiringAssistanceSpecialMeasures_valuesOtherThanYesNo(){
        final List<Witnesses> witnesses = new ArrayList<>();
        Witnesses witness = Witnesses.witnesses()
                .withForename("Robert")
                .withSurname("Drane")
                .withInterpreterRequired(false)
                .withWelshLanguage(false)
                .withProsecutionWitnessRequiringAssistanceSpecialMeasures("T")
                .withProsecutionRequireAttendance(true)
                .build();
        witnesses.add(witness);
        CpsServePet.Builder builder = createCpsServePetPayload();
        CpsServePet cpsServePet = builder.withWitnesses(witnesses).build();
        final Map<String, List<String>> actualValidations = petValidator.validate(cpsServePet, new HashMap<>());

        final String errorMsg = "Invalid value.";
        final List<Pair<String, String>> errors = newArrayList(
                Pair.of(FieldName.WITNESS_PROSECUTION_WITNESS_REQUIRING_ASSISTANCE_SPECIAL_MEASURES.getValue(), errorMsg));

        thenValidationFailsWith(actualValidations, errors);
    }

    @Test
    public void validateProsecutionWitnessRequiringAssistanceSpecialMeasures(){
        final List<Witnesses> witnesses = new ArrayList<>();
        Witnesses witness = Witnesses.witnesses()
                .withForename("Robert")
                .withSurname("Drane")
                .withInterpreterRequired(false)
                .withWelshLanguage(false)
                .withProsecutionWitnessRequiringAssistanceSpecialMeasures("N")
                .withProsecutionRequireAttendance(true)
                .withDateOfBirth("1982-01-01")
                .build();
        witnesses.add(witness);
        CpsServePet.Builder builder = createCpsServePetPayload();
        CpsServePet cpsServePet = builder.withWitnesses(witnesses).build();
        final Map<String, List<String>> actualValidations = petValidator.validate(cpsServePet, new HashMap<>());

        final List<Pair<String, String>> errors = newArrayList();

        thenValidationFailsWith(actualValidations, errors);
    }

    private CpsServePet.Builder createCpsServePetPayload() {
        List<DefendantOffencesSubjects> defendantOffencesSubjects = new ArrayList<>();
        final List<OffenceSubject> offence = new ArrayList<>();
        final List<ProsecutionWillRelyOn> prosecutionWillRelyOn = new ArrayList<>();
        prosecutionWillRelyOn.add(ProsecutionWillRelyOn.CCTV_EVIDENCE);

        offence.add(OffenceSubject.offenceSubject()
                .withCjsOffenceCode("CJSCODE001")
                .withOffenceDate("2021-09-27")
                .withOffenceWording("Test Offence Wording")
                .build());

        defendantOffencesSubjects.add(DefendantOffencesSubjects.defendantOffencesSubjects()
                .withDefendant(DefendantSubject.defendantSubject()
                .withProsecutorDefendantId("PDEFID001")
                .build())
                .withOffences(offence)
                .build());

        return CpsServePet.cpsServePet()
                .withProsecutionCaseSubject(CpsProsecutionCaseSubject.cpsProsecutionCaseSubject()
                        .withUrn("TEST001")
                        .withProsecutingAuthority("OUCODE")
                        .build())
                .withDefendantOffencesSubjects(defendantOffencesSubjects)
                .withAreThereanypendingEnquiriesorLinesOfInvestigation(AreThereanypendingEnquiriesorLinesOfInvestigation.N)
                .withCourtToArrangeADiscussionOfGroundRulesForQuestioning(CourtToArrangeADiscussionOfGroundRulesForQuestioning.N)
                .withDoesTheProsecutorIntendToServeMoreEvidence(DoesTheProsecutorIntendToServeMoreEvidence.N)
                .withHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith(HasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith.N)
                .withHasDefendantHasBeenAVictimOfSlaveryOrExploitation(HasDefendantHasBeenAVictimOfSlaveryOrExploitation.N)
                .withVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirection(VaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirection.N)
                .withExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFact(ExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFact.N)
                .withProsecutionWillRelyOn(prosecutionWillRelyOn)
                .withWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoom(WillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoom.N)
                .withParentGuardianToAttend(false);
    }

    private void thenValidationFailsWith(final Map<String, List<String>> actualValidations, final List<Pair<String, String>> errors) {
        final Map<String, List<String>> violationsMap = new HashMap<>();
        errors.forEach(error -> violationsMap.put(error.getKey(), singletonList(error.getValue())));
        assertThat(actualValidations, equalTo(violationsMap));
    }
}
