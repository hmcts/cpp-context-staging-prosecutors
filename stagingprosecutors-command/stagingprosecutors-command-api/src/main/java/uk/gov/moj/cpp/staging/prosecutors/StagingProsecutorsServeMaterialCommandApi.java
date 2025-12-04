package uk.gov.moj.cpp.staging.prosecutors;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServeBcm;
import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServeBcmWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServeCotr;
import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServeCotrWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServePet;
import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServePetWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServePtph;
import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServePtphWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsUpdateCotr;
import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsUpdateCotrWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.uuid.UUIDProducer;
import uk.gov.moj.cpp.staging.prosecutors.validators.BcmValidator;
import uk.gov.moj.cpp.staging.prosecutors.validators.CotrValidator;
import uk.gov.moj.cpp.staging.prosecutors.validators.PetValidator;
import uk.gov.moj.cpp.staging.prosecutors.validators.PtphValidator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_API)
public class StagingProsecutorsServeMaterialCommandApi {

    private static final String RESPONSE_URL_VERSION_PLACEHOLDER = "VERSION";
    private static final String VERSION_NO = "v1";
    private static final Logger LOGGER = LoggerFactory.getLogger(StagingProsecutorsServeMaterialCommandApi.class);
    private final ObjectMapper mapper = new ObjectMapperProducer().objectMapper().copy();
    @Inject
    @Value(key = "stagingprosecutors.submit-cps-serve-pet-response.base-url", defaultValue = "https://replace-me.gov.uk/")
    String baseResponseURL;
    @Inject
    private Sender sender;
    @Inject
    private PetValidator petValidator;
    @Inject
    private CotrValidator cotrValidator;
    @Inject
    private BcmValidator bcmValidator;
    @Inject
    private PtphValidator ptphValidator;
    @Inject
    private UUIDProducer uuidProducer;

    @Handles("stagingprosecutors.cps-serve-pet")
    public Envelope<UrlResponse> cpsServePet(final Envelope<CpsServePet> cpsServePetEnvelope) {
        LOGGER.info("stagingprosecutors.cps-serve-pet..");

        final Map<String, List<String>> violations = new HashMap<>();
        final CpsServePet cpsServePet = cpsServePetEnvelope.payload();

        petValidator.validate(cpsServePet, violations);

        if (violations.size() > 0) {
            throwBadRequestException(violations);
        }

        final UUID submissionId = uuidProducer.generateUUID();

        final CpsServePetWithSubmissionId payloadWithSubmissionId = CpsServePetWithSubmissionId.cpsServePetWithSubmissionId()
                .withSubmissionId(submissionId)
                .withProsecutionCaseSubject(cpsServePet.getProsecutionCaseSubject())
                .withDefendantOffencesSubjects(cpsServePet.getDefendantOffencesSubjects())
                .withAreThereAnyPendingEnquiriesOrLinesOfInvestigation(cpsServePet.getAreThereanypendingEnquiriesorLinesOfInvestigation().toString())
                .withAreThereAnyPendingEnquiriesOrLinesOfInvestigationDetails(cpsServePet.getAreThereanypendingEnquiriesorLinesOfInvestigationDetais())
                .withCourtToArrangeADiscussionOfGroundRulesForQuestioning(cpsServePet.getCourtToArrangeADiscussionOfGroundRulesForQuestioning().toString())
                .withDoesTheProsecutorIntendToServeMoreEvidence(cpsServePet.getDoesTheProsecutorIntendToServeMoreEvidence().toString())
                .withDoesTheProsecutorIntendToServeMoreEvidenceDetails(cpsServePet.getDoesTheProsecutorIntendToServeMoreEvidenceDetails())
                .withExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFact(cpsServePet.getExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFact().toString())
                .withExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFactDetails(cpsServePet.getExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFactDetails())
                .withHasDefendantHasBeenAVictimOfSlaveryOrExploitation(cpsServePet.getHasDefendantHasBeenAVictimOfSlaveryOrExploitation().toString())
                .withHasDefendantHasBeenAVictimOfSlaveryOrExploitationDetails(cpsServePet.getHasDefendantHasBeenAVictimOfSlaveryOrExploitationDetails())
                .withHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith(cpsServePet.getHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith().toString())
                .withHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWithStateWhenThisWas(cpsServePet.getHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWithStateWhenThisWas())
                .withParentGuardianToAttend(cpsServePet.getParentGuardianToAttend())
                .withProsecutionWillRelyOn(cpsServePet.getProsecutionWillRelyOn())
                .withVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirection(cpsServePet.getVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirection().toString())
                .withVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirectionDetails(cpsServePet.getVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirectionDetails())
                .withWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoom(cpsServePet.getWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoom().toString())
                .withWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoomDetails(cpsServePet.getWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoomDetails())
                .withWitnesses(cpsServePet.getWitnesses())
                .withTag(cpsServePet.getTag())
                .withPetAdvocate(cpsServePet.getPetAdvocate())
                .withTrialAdvocate(cpsServePet.getTrialAdvocate())
                .withReviewingLawyer(cpsServePet.getReviewingLawyer())
                .withProsecutionCaseProgressionOfficer(cpsServePet.getProsecutionCaseProgressionOfficer())
                .withOfficerInTheCase(cpsServePet.getOfficerInTheCase())
                .withAdditionalInformation(cpsServePet.getAdditionalInformation())
                .withIsYouth(cpsServePet.getIsYouth())
                .build();
        sender.send(envelop(payloadWithSubmissionId)
                .withName("stagingprosecutors.command.submit-cps-serve-pet")
                .withMetadataFrom(cpsServePetEnvelope));

        return Envelope.envelopeFrom(cpsServePetEnvelope.metadata(), new UrlResponse(getBaseResponseURLWithVersion() + submissionId.toString(), submissionId));
    }

    @Handles("stagingprosecutors.cps-serve-bcm")
    public Envelope<UrlResponse> cpsServeBcm(final Envelope<CpsServeBcm> cpsServeBcmEnvelope) {
        LOGGER.info("stagingprosecutors.cps-serve-bcm..");

        final Map<String, List<String>> violations = new HashMap<>();
        final CpsServeBcm cpsServeBcm = cpsServeBcmEnvelope.payload();

        bcmValidator.validate(cpsServeBcm, violations);

        if (violations.size() > 0) {
            throwBadRequestException(violations);
        }

        final UUID submissionId = uuidProducer.generateUUID();

        final CpsServeBcmWithSubmissionId payloadWithSubmissionId = CpsServeBcmWithSubmissionId.cpsServeBcmWithSubmissionId()
                .withSubmissionId(submissionId)
                .withProsecutionCaseSubject(cpsServeBcm.getProsecutionCaseSubject())
                .withDefendantOffencesSubject(cpsServeBcm.getDefendantOffencesSubject())
                .withEvidencePostPTPH(cpsServeBcm.getEvidencePostPTPH())
                .withEvidencePrePTPH(cpsServeBcm.getEvidencePrePTPH())
                .withOtherInformation(cpsServeBcm.getOtherInformation())
                .withTag(cpsServeBcm.getTag())
                .build();
        sender.send(envelop(payloadWithSubmissionId)
                .withName("stagingprosecutors.command.submit-cps-serve-bcm")
                .withMetadataFrom(cpsServeBcmEnvelope));

        return Envelope.envelopeFrom(cpsServeBcmEnvelope.metadata(), new UrlResponse(getBaseResponseURLWithVersion() + submissionId.toString(), submissionId));
    }

    @Handles("stagingprosecutors.cps-serve-ptph")
    public Envelope<UrlResponse> cpsServePtph(final Envelope<CpsServePtph> cpsServePtphEnvelope) {
        LOGGER.info("stagingprosecutors.cps-serve-ptph");

        final CpsServePtph cpsServePtph = cpsServePtphEnvelope.payload();
        final Map<String, List<String>> violations = new HashMap<>();

        ptphValidator.validate(cpsServePtph, violations);

        if (violations.size() > 0) {
            throwBadRequestException(violations);
        }

        final UUID submissionId = uuidProducer.generateUUID();

        final CpsServePtphWithSubmissionId payloadWithSubmissionId = CpsServePtphWithSubmissionId.cpsServePtphWithSubmissionId()
                .withSubmissionId(submissionId)
                .withProsecutionCaseSubject(cpsServePtph.getProsecutionCaseSubject())
                .withDefendantOffencesSubjects(cpsServePtph.getDefendantOffencesSubjects())
                .withBadCharacter(cpsServePtph.getBadCharacter())
                .withBadCharacterNotes(cpsServePtph.getBadCharacterNotes())
                .withCctv(cpsServePtph.getCctv())
                .withCctvNotes(cpsServePtph.getCctvNotes())
                .withCpsOffice(cpsServePtph.getCpsOffice())
                .withCriminalRecord(cpsServePtph.getCriminalRecord())
                .withCriminalRecordNotes(cpsServePtph.getCriminalRecordNotes())
                .withDisclosureManagementDoc(cpsServePtph.getDisclosureManagementDoc())
                .withDisclosureManagementDocNotes(cpsServePtph.getDisclosureManagementDocNotes())
                .withDraftIndictment(cpsServePtph.getDraftIndictment())
                .withDraftIndictmentNotes(cpsServePtph.getDraftIndictmentNotes())
                .withExhibitsForPAndICm(cpsServePtph.getExhibitsForPAndICm())
                .withExhibitsForPAndICmNotes(cpsServePtph.getExhibitsForPAndICmNotes())
                .withExpertEvidence(cpsServePtph.getExpertEvidence())
                .withExpertEvidenceNotes(cpsServePtph.getExpertEvidenceNotes())
                .withHearsay(cpsServePtph.getHearsay())
                .withHearsayNotes(cpsServePtph.getHearsayNotes())
                .withMedicalEvidence(cpsServePtph.getMedicalEvidence())
                .withMedicalEvidenceNotes(cpsServePtph.getMedicalEvidenceNotes())
                .withOfficerInTheCase(cpsServePtph.getOfficerInTheCase())
                .withParticularsOfAnyFamily(cpsServePtph.getParticularsOfAnyFamily())
                .withParticularsOfAnyRelatedCriminalProceedings(cpsServePtph.getParticularsOfAnyRelatedCriminalProceedings())
                .withProsecutionCaseProgressionOfficer(cpsServePtph.getProsecutionCaseProgressionOfficer())
                .withPtphAdvocate(cpsServePtph.getPtphAdvocate())
                .withReviewDisclosableMaterial(cpsServePtph.getReviewDisclosableMaterial())
                .withReviewDisclosableMaterialNotes(cpsServePtph.getReviewDisclosableMaterialNotes())
                .withReviewingLawyer(cpsServePtph.getReviewingLawyer())
                .withSpecialMeasures(cpsServePtph.getSpecialMeasures())
                .withSpecialMeasuresNotes(cpsServePtph.getSpecialMeasuresNotes())
                .withStatementsForPAndICm(cpsServePtph.getStatementsForPAndICm())
                .withStatementsForPAndICmNotes(cpsServePtph.getStatementsForPAndICmNotes())
                .withStreamlinedForensicReport(cpsServePtph.getStreamlinedForensicReport())
                .withStreamlinedForensicReportNotes(cpsServePtph.getStreamlinedForensicReportNotes())
                .withSummaryOfCircumstances(cpsServePtph.getSummaryOfCircumstances())
                .withSummaryOfCircumstancesNotes(cpsServePtph.getSummaryOfCircumstancesNotes())
                .withTag(cpsServePtph.getTag())
                .withThirdParty(cpsServePtph.getThirdParty())
                .withThirdPartyNotes(cpsServePtph.getThirdPartyNotes())
                .withTrialAdvocate(cpsServePtph.getTrialAdvocate())
                .withVictimPersonalStatement(cpsServePtph.getVictimPersonalStatement())
                .withVictimPersonalStatementNotes(cpsServePtph.getVictimPersonalStatementNotes())
                .withWitnesses(cpsServePtph.getWitnesses())
                .build();

        sender.send(envelop(payloadWithSubmissionId)
                .withName("stagingprosecutors.command.submit-cps-serve-ptph")
                .withMetadataFrom(cpsServePtphEnvelope));

        return Envelope.envelopeFrom(cpsServePtphEnvelope.metadata(),
                new UrlResponse(getBaseResponseURLWithVersion() + submissionId.toString(), submissionId));
    }

    @Handles("stagingprosecutors.cps-update-cotr")
    public Envelope<UrlResponse> cpsUpdateCotr(final Envelope<CpsUpdateCotr> cpsUpdateCotrEnvelope) {
        LOGGER.info("stagingprosecutors.cps-update-cotr..");

        final CpsUpdateCotr cpsUpdateCotr = cpsUpdateCotrEnvelope.payload();

        final UUID submissionId = uuidProducer.generateUUID();

        final CpsUpdateCotrWithSubmissionId payloadWithSubmissionId = CpsUpdateCotrWithSubmissionId.cpsUpdateCotrWithSubmissionId()
                .withSubmissionId(submissionId)
                .withCotrId(cpsUpdateCotr.getCotrSubmissionId())
                .withProsecutionCaseSubject(cpsUpdateCotr.getProsecutionCaseSubject())
                .withDefendantSubject(cpsUpdateCotr.getDefendantSubject())
                .withCertifyThatTheProsecutionIsTrialReady(cpsUpdateCotr.getCertifyThatTheProsecutionIsTrialReady().toString())
                .withTrialDate(cpsUpdateCotr.getTrialDate())
                .withDate(cpsUpdateCotr.getDate())
                .withFormCompletedOnBehalfOfProsecutionBy(cpsUpdateCotr.getFormCompletedOnBehalfOfProsecutionBy())
                .withFurtherProsecutionInformationProvidedAfterCertification(cpsUpdateCotr.getFurtherProsecutionInformationProvidedAfterCertification())
                .build();

        sender.send(envelop(payloadWithSubmissionId)
                .withName("stagingprosecutors.command.submit-cps-update-cotr")
                .withMetadataFrom(cpsUpdateCotrEnvelope));

        return Envelope.envelopeFrom(cpsUpdateCotrEnvelope.metadata(),
                new UrlResponse(getBaseResponseURLWithVersion() + submissionId.toString(), submissionId));
    }

    @Handles("stagingprosecutors.cps-serve-cotr")
    public Envelope<UrlResponse> cpsServeCotr(final Envelope<CpsServeCotr> cpsServeCotrEnvelope) {
        LOGGER.info("stagingprosecutors.cps-serve-cotr..");

        final Map<String, List<String>> violations = new HashMap<>();
        final CpsServeCotr cpsServeCotr = cpsServeCotrEnvelope.payload();

        cotrValidator.validate(cpsServeCotr, violations);

        if (violations.size() > 0) {
            throwBadRequestException(violations);
        }

        final UUID submissionId = uuidProducer.generateUUID();

        final CpsServeCotrWithSubmissionId payloadWithSubmissionId = CpsServeCotrWithSubmissionId.cpsServeCotrWithSubmissionId()
                .withSubmissionId(submissionId)
                .withApplyForThePtrToBeVacated(cpsServeCotr.getApplyForThePtrToBeVacated().toString())
                .withApplyForThePtrToBeVacatedDetails(cpsServeCotr.getApplyForThePtrToBeVacatedDetails())
                .withCertificationDate(cpsServeCotr.getCertificationDate())
                .withCertifyThatTheProsecutionIsTrialReady(cpsServeCotr.getCertifyThatTheProsecutionIsTrialReady().toString())
                .withCertifyThatTheProsecutionIsTrialReadyDetails(cpsServeCotr.getCertifyThatTheProsecutionIsTrialReadyDetails())
                .withDefendantSubject(cpsServeCotr.getDefendantSubject())
                .withFormCompletedOnBehalfOfTheProsecutionBy(cpsServeCotr.getFormCompletedOnBehalfOfTheProsecutionBy())
                .withFurtherInformationToAssistTheCourt(cpsServeCotr.getFurtherInformationToAssistTheCourt())
                .withProsecutionCaseSubject(cpsServeCotr.getProsecutionCaseSubject())
                .withTrialDate(cpsServeCotr.getTrialDate())
                .withLastRecordedTimeEstimate(cpsServeCotr.getLastRecordedTimeEstimate())
                .withHasAllEvidenceToBeReliedOnBeenServed(cpsServeCotr.getHasAllEvidenceToBeReliedOnBeenServed().toString())
                .withHasAllEvidenceToBeReliedOnBeenServedDetails(cpsServeCotr.getHasAllEvidenceToBeReliedOnBeenServedDetails())
                .withHasAllDisclosureBeenProvided(cpsServeCotr.getHasAllDisclosureBeenProvided().toString())
                .withHasAllDisclosureBeenProvidedDetails(cpsServeCotr.getHasAllDisclosureBeenProvidedDetails())
                .withHaveOtherDirectionsBeenCompliedWith(cpsServeCotr.getHaveOtherDirectionsBeenCompliedWith().toString())
                .withHaveOtherDirectionsBeenCompliedWithDetails(cpsServeCotr.getHaveOtherDirectionsBeenCompliedWithDetails())
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend(cpsServeCotr.getHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend().toString())
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails(cpsServeCotr.getHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails())
                .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed(cpsServeCotr.getHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed().toString())
                .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServedDetails(cpsServeCotr.getHaveAnyWitnessSummonsesRequiredBeenReceivedAndServedDetails())
                .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved(cpsServeCotr.getHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved().toString())
                .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolvedDetails(cpsServeCotr.getHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolvedDetails())
                .withHaveInterpretersForWitnessesBeenArranged(cpsServeCotr.getHaveInterpretersForWitnessesBeenArranged().toString())
                .withHaveInterpretersForWitnessesBeenArrangedDetails(cpsServeCotr.getHaveInterpretersForWitnessesBeenArrangedDetails())
                .withHaveEditedAbeInterviewsBeenPreparedAndAgreed(cpsServeCotr.getHaveEditedAbeInterviewsBeenPreparedAndAgreed().toString())
                .withHaveEditedAbeInterviewsBeenPreparedAndAgreedDetails(cpsServeCotr.getHaveEditedAbeInterviewsBeenPreparedAndAgreedDetails())
                .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement(cpsServeCotr.getHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement().toString())
                .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreementDetails(cpsServeCotr.getHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreementDetails())
                .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJury(cpsServeCotr.getIsTheCaseReadyToProceedWithoutDelayBeforeTheJury().toString())
                .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJuryDetails(cpsServeCotr.getIsTheCaseReadyToProceedWithoutDelayBeforeTheJuryDetails())
                .withIsTheTimeEstimateCorrect(cpsServeCotr.getIsTheTimeEstimateCorrect().toString())
                .withIsTheTimeEstimateCorrectDetails(cpsServeCotr.getIsTheTimeEstimateCorrectDetails())
                .build();
        sender.send(envelop(payloadWithSubmissionId)
                .withName("stagingprosecutors.command.submit-cps-serve-cotr")
                .withMetadataFrom(cpsServeCotrEnvelope));

        return Envelope.envelopeFrom(cpsServeCotrEnvelope.metadata(), new UrlResponse(getBaseResponseURLWithVersion() + submissionId.toString(), submissionId));
    }

    private void throwBadRequestException(final Map<String, List<String>> violations) {
        try {
            throw new BadRequestException(mapper.writeValueAsString(violations));
        } catch (final JsonProcessingException e) {
            LOGGER.error("Unable to serialize violations json object", e);
            throw new BadRequestException("Business validations failed");
        }
    }

    private String getBaseResponseURLWithVersion() {
        return this.baseResponseURL.replace(RESPONSE_URL_VERSION_PLACEHOLDER, VERSION_NO);
    }
}
