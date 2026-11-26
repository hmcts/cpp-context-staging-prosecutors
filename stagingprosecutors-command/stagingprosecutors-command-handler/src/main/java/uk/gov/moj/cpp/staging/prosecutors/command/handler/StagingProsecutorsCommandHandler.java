package uk.gov.moj.cpp.staging.prosecutors.command.handler;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import org.slf4j.LoggerFactory;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.command.SubmitApplication;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.WebApplicationException;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplicationType;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitCpsServeBcm;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.converter.ChargeDefendantConverter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.converter.ChargeProsecutionSubmissionDetailsConverter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.converter.DocxToCourtApplicationConverter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.service.ReferenceDataServiceImpl;
import uk.gov.moj.cpp.staging.prosecutors.domain.CpsSubmission;
import uk.gov.moj.cpp.staging.prosecutors.domain.PocaEmailAggregate;
import uk.gov.moj.cpp.staging.prosecutors.domain.ProsecutionSubmission;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeBcmReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePtphReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsUpdateCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSubmissionSuccessfulWithWarnings;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.RejectSubmission;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecution;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.UpdateSubmissionStatus;

import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ReceivePocaEmail;

@SuppressWarnings("squid:S1160")
@ServiceComponent(COMMAND_HANDLER)
public class StagingProsecutorsCommandHandler {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(StagingProsecutorsCommandHandler.class);

    private static final String POCA_RESPONDENT_DETAILS_MISSING = "poca_respondent_details_missing";

    private final ChargeProsecutionSubmissionDetailsConverter chargeProsecutionSubmissionDetailsConverter = new ChargeProsecutionSubmissionDetailsConverter();
    private final ChargeDefendantConverter chargeDefendantConverter = new ChargeDefendantConverter();
    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;
    @Inject
    private FileRetriever fileRetriever;

    @Inject
    private ReferenceDataServiceImpl referenceDataServiceImpl;

    @Handles("stagingprosecutors.command.charge-prosecution")
    public void handleChargeProsecutionSubmission(final Envelope<SubmitChargeProsecution> envelope) throws EventStreamException {

        final SubmitChargeProsecution submitChargeProsecution = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(submitChargeProsecution.getSubmissionId());
        final ProsecutionSubmission prosecutionSubmission = aggregateService.get(eventStream, ProsecutionSubmission.class);

        final Stream<Object> events = prosecutionSubmission.receiveSubmission(
                submitChargeProsecution.getSubmissionId(),
                chargeProsecutionSubmissionDetailsConverter.convert(submitChargeProsecution.getProsecutionSubmissionDetails()),
                chargeDefendantConverter.convert(submitChargeProsecution.getDefendants()));

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("stagingprosecutors.command.submit-cps-serve-pet")
    public void handleServePet(final Envelope<SubmitCpsServePet> submitCpsServePetEnvelope) throws EventStreamException {

        final SubmitCpsServePet submitCpsServePet = submitCpsServePetEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(submitCpsServePet.getSubmissionId());
        final CpsSubmission cpsSubmission = aggregateService.get(eventStream, CpsSubmission.class);

        final Stream<Object> events = cpsSubmission.receivePetSubmission(convertSubmitCpsServePet(submitCpsServePet));
        appendEventsToStream(submitCpsServePetEnvelope, eventStream, events);
    }

    @SuppressWarnings("squid:S3655")
    @Handles("stagingprosecutors.command.submit-cps-serve-bcm")
    public void handleServeBcm(final Envelope<SubmitCpsServeBcm> submitCpsServeBcmEnvelope) throws EventStreamException {

        final SubmitCpsServeBcm submitCpsServeBcm = submitCpsServeBcmEnvelope.payload();
        if (nonNull(submitCpsServeBcm.getSubmissionId())) {
            final EventStream eventStream = eventSource.getStreamById(submitCpsServeBcm.getSubmissionId());
            final CpsSubmission cpsSubmission = aggregateService.get(eventStream, CpsSubmission.class);

            final CpsServeBcmReceived cpsServeBcmReceived = CpsServeBcmReceived.cpsServeBcmReceived()
                    .withSubmissionId(submitCpsServeBcm.getSubmissionId())
                    .withDefendantOffencesSubject(submitCpsServeBcm.getDefendantOffencesSubject())
                    .withProsecutionCaseSubject(submitCpsServeBcm.getProsecutionCaseSubject())
                    .withEvidencePrePTPH(submitCpsServeBcm.getEvidencePrePTPH())
                    .withEvidencePostPTPH(submitCpsServeBcm.getEvidencePostPTPH())
                    .withOtherInformation(submitCpsServeBcm.getOtherInformation())
                    .withTag(submitCpsServeBcm.getTag())
                    .build();

            final Stream<Object> events = cpsSubmission.receiveBcmSubmission(cpsServeBcmReceived);
            appendEventsToStream(submitCpsServeBcmEnvelope, eventStream, events);
        }
    }

    @Handles("stagingprosecutors.command.submit-cps-serve-ptph")
    public void handleServePtph(final Envelope<SubmitCpsServePtph> submitCpsServePtphEnvelope) throws EventStreamException {

        final SubmitCpsServePtph submitCpsServePtph = submitCpsServePtphEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(submitCpsServePtph.getSubmissionId());
        final CpsSubmission cpsSubmission = aggregateService.get(eventStream, CpsSubmission.class);

        final Stream<Object> events = cpsSubmission.receivePtphSubmission(convertSubmitCpsServePtph(submitCpsServePtph));
        appendEventsToStream(submitCpsServePtphEnvelope, eventStream, events);
    }

    @Handles("stagingprosecutors.command.submit-cps-serve-cotr")
    public void handleServeCotr(final Envelope<SubmitCpsServeCotr> submitCpsServeCotrEnvelope) throws EventStreamException {

        final SubmitCpsServeCotr submitCpsServeCotr = submitCpsServeCotrEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(submitCpsServeCotr.getSubmissionId());
        final CpsSubmission cpsSubmission = aggregateService.get(eventStream, CpsSubmission.class);

        final Stream<Object> events = cpsSubmission.receiveCotrSubmission(convertSubmitCpsServeCotr(submitCpsServeCotr));
        appendEventsToStream(submitCpsServeCotrEnvelope, eventStream, events);
    }

    @Handles("stagingprosecutors.command.submit-cps-update-cotr")
    public void handleUpdateCotr(final Envelope<SubmitCpsUpdateCotr> submitCpsUpdateCotrEnvelope) throws EventStreamException {

        final SubmitCpsUpdateCotr submitCpsUpdateCotr = submitCpsUpdateCotrEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(submitCpsUpdateCotr.getSubmissionId());
        final CpsSubmission cpsSubmission = aggregateService.get(eventStream, CpsSubmission.class);

        final Stream<Object> events = cpsSubmission.receiveUpdateCotrSubmission(convertSubmitCpsUpdateCotr(submitCpsUpdateCotr));
        appendEventsToStream(submitCpsUpdateCotrEnvelope, eventStream, events);
    }

    @Handles("stagingprosecutors.command.sjp-prosecution")
    public void handleSjpProsecutionSubmission(final Envelope<SubmitSjpProsecution> envelope) throws EventStreamException {

        final SubmitSjpProsecution submitSjpProsecution = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(submitSjpProsecution.getSubmissionId());
        final ProsecutionSubmission prosecutionSubmission = aggregateService.get(eventStream, ProsecutionSubmission.class);

        final Stream<Object> events = prosecutionSubmission.receiveSjpSubmission(
                submitSjpProsecution.getSubmissionId(),
                submitSjpProsecution.getProsecutionSubmissionDetails(),
                submitSjpProsecution.getDefendant());

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("stagingprosecutors.command.receive-submission-successful")
    public void handleReceiveSubmissionSuccessful(final Envelope<ReceiveSubmissionSuccessful> envelope) throws EventStreamException {
        final ReceiveSubmissionSuccessful receiveSubmissionSuccessful = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(receiveSubmissionSuccessful.getSubmissionId());
        final ProsecutionSubmission prosecutionSubmission = aggregateService.get(eventStream, ProsecutionSubmission.class);

        final Stream<Object> events = prosecutionSubmission.receiveSubmissionSuccessful(
                receiveSubmissionSuccessful.getSubmissionId());

        appendEventsToStream(envelope, eventStream, events);

    }

    @Handles("stagingprosecutors.command.receive-submission-successful-with-warnings")
    public void handleReceiveSubmissionSuccessfulWithWarnings(final Envelope<ReceiveSubmissionSuccessfulWithWarnings> envelope) throws EventStreamException {
        final ReceiveSubmissionSuccessfulWithWarnings receiveSubmissionSuccessfulWithWarnings = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(receiveSubmissionSuccessfulWithWarnings.getSubmissionId());
        final ProsecutionSubmission prosecutionSubmission = aggregateService.get(eventStream, ProsecutionSubmission.class);

        final Stream<Object> events = prosecutionSubmission.receiveSubmissionSuccessfulWithWarnings(
                receiveSubmissionSuccessfulWithWarnings.getSubmissionId(),
                receiveSubmissionSuccessfulWithWarnings.getWarnings(),
                receiveSubmissionSuccessfulWithWarnings.getDefendantWarnings()
        );

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("stagingprosecutors.command.reject-submission")
    public void handleRejectSubmission(final Envelope<RejectSubmission> envelope) throws EventStreamException {
        final RejectSubmission rejectSubmission = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(rejectSubmission.getSubmissionId());
        final ProsecutionSubmission prosecutionSubmission = aggregateService.get(eventStream, ProsecutionSubmission.class);

        final Stream<Object> events = prosecutionSubmission.receiveSubmissionRejection(
                rejectSubmission.getSubmissionId(),
                rejectSubmission.getErrors(),
                rejectSubmission.getCaseErrors(),
                rejectSubmission.getDefendantErrors());

        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    @SuppressWarnings({"squid:S3776", "squid:S1067", "squid:S1188"})
    private CpsServePetReceived convertSubmitCpsServePet(final SubmitCpsServePet submitCpsServePet) {
        return CpsServePetReceived.cpsServePetReceived()
                .withSubmissionId(submitCpsServePet.getSubmissionId())
                .withProsecutionCaseSubject(submitCpsServePet.getProsecutionCaseSubject())
                .withDefendantOffencesSubjects(submitCpsServePet.getDefendantOffencesSubjects())
                .withAreThereAnyPendingEnquiriesOrLinesOfInvestigation(submitCpsServePet.getAreThereAnyPendingEnquiriesOrLinesOfInvestigation())
                .withAreThereAnyPendingEnquiriesOrLinesOfInvestigationDetails(submitCpsServePet.getAreThereAnyPendingEnquiriesOrLinesOfInvestigationDetails())
                .withCourtToArrangeADiscussionOfGroundRulesForQuestioning(submitCpsServePet.getCourtToArrangeADiscussionOfGroundRulesForQuestioning())
                .withDoesTheProsecutorIntendToServeMoreEvidence(submitCpsServePet.getDoesTheProsecutorIntendToServeMoreEvidence())
                .withDoesTheProsecutorIntendToServeMoreEvidenceDetails(submitCpsServePet.getDoesTheProsecutorIntendToServeMoreEvidenceDetails())
                .withExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFact(submitCpsServePet.getExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFact())
                .withExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFactDetails(submitCpsServePet.getExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFactDetails())
                .withHasDefendantHasBeenAVictimOfSlaveryOrExploitation(submitCpsServePet.getHasDefendantHasBeenAVictimOfSlaveryOrExploitation())
                .withHasDefendantHasBeenAVictimOfSlaveryOrExploitationDetails(submitCpsServePet.getHasDefendantHasBeenAVictimOfSlaveryOrExploitationDetails())
                .withHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith(submitCpsServePet.getHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith())
                .withHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWithStateWhenThisWas(submitCpsServePet.getHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWithStateWhenThisWas())
                .withParentGuardianToAttend(submitCpsServePet.getParentGuardianToAttend())
                .withProsecutionWillRelyOn(submitCpsServePet.getProsecutionWillRelyOn())
                .withVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirection(submitCpsServePet.getVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirection())
                .withVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirectionDetails(submitCpsServePet.getVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirectionDetails())
                .withWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoom(submitCpsServePet.getWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoom())
                .withWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoomDetails(submitCpsServePet.getWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoomDetails())
                .withWitnesses(submitCpsServePet.getWitnesses())
                .withTag(submitCpsServePet.getTag())
                .withPetAdvocate(submitCpsServePet.getPetAdvocate())
                .withTrialAdvocate(submitCpsServePet.getTrialAdvocate())
                .withProsecutionCaseProgressionOfficer(submitCpsServePet.getProsecutionCaseProgressionOfficer())
                .withReviewingLawyer(submitCpsServePet.getReviewingLawyer())
                .withOfficerInTheCase(submitCpsServePet.getOfficerInTheCase())
                .withProsecutionWillRelyOn(submitCpsServePet.getProsecutionWillRelyOn())
                .withAdditionalInformation(submitCpsServePet.getAdditionalInformation())
                .withIsYouth(submitCpsServePet.getIsYouth())
                .build();
    }

    private CpsServePtphReceived convertSubmitCpsServePtph(final SubmitCpsServePtph submitCpsServePtph) {
        return CpsServePtphReceived.cpsServePtphReceived()
                .withSubmissionId(submitCpsServePtph.getSubmissionId())
                .withProsecutionCaseSubject(submitCpsServePtph.getProsecutionCaseSubject())
                .withDefendantOffencesSubjects(submitCpsServePtph.getDefendantOffencesSubjects())
                .withBadCharacter(submitCpsServePtph.getBadCharacter())
                .withBadCharacterNotes(submitCpsServePtph.getBadCharacterNotes())
                .withCctv(submitCpsServePtph.getCctv())
                .withCctvNotes(submitCpsServePtph.getCctvNotes())
                .withCpsOffice(submitCpsServePtph.getCpsOffice())
                .withCriminalRecord(submitCpsServePtph.getCriminalRecord())
                .withCriminalRecordNotes(submitCpsServePtph.getCriminalRecordNotes())
                .withDisclosureManagementDoc(submitCpsServePtph.getDisclosureManagementDoc())
                .withDisclosureManagementDocNotes(submitCpsServePtph.getDisclosureManagementDocNotes())
                .withDraftIndictment(submitCpsServePtph.getDraftIndictment())
                .withDraftIndictmentNotes(submitCpsServePtph.getDraftIndictmentNotes())
                .withExhibitsForPAndICm(submitCpsServePtph.getExhibitsForPAndICm())
                .withExhibitsForPAndICmNotes(submitCpsServePtph.getExhibitsForPAndICmNotes())
                .withExpertEvidence(submitCpsServePtph.getExpertEvidence())
                .withExpertEvidenceNotes(submitCpsServePtph.getExpertEvidenceNotes())
                .withHearsay(submitCpsServePtph.getHearsay())
                .withHearsayNotes(submitCpsServePtph.getHearsayNotes())
                .withMedicalEvidence(submitCpsServePtph.getMedicalEvidence())
                .withMedicalEvidenceNotes(submitCpsServePtph.getMedicalEvidenceNotes())
                .withOfficerInTheCase(submitCpsServePtph.getOfficerInTheCase())
                .withParticularsOfAnyFamily(submitCpsServePtph.getParticularsOfAnyFamily())
                .withParticularsOfAnyRelatedCriminalProceedings(submitCpsServePtph.getParticularsOfAnyRelatedCriminalProceedings())
                .withProsecutionCaseProgressionOfficer(submitCpsServePtph.getProsecutionCaseProgressionOfficer())
                .withPtphAdvocate(submitCpsServePtph.getPtphAdvocate())
                .withReviewDisclosableMaterial(submitCpsServePtph.getReviewDisclosableMaterial())
                .withReviewDisclosableMaterialNotes(submitCpsServePtph.getReviewDisclosableMaterialNotes())
                .withReviewingLawyer(submitCpsServePtph.getReviewingLawyer())
                .withSpecialMeasures(submitCpsServePtph.getSpecialMeasures())
                .withSpecialMeasuresNotes(submitCpsServePtph.getSpecialMeasuresNotes())
                .withStatementsForPAndICm(submitCpsServePtph.getStatementsForPAndICm())
                .withStatementsForPAndICmNotes(submitCpsServePtph.getStatementsForPAndICmNotes())
                .withStreamlinedForensicReport(submitCpsServePtph.getStreamlinedForensicReport())
                .withStreamlinedForensicReportNotes(submitCpsServePtph.getStreamlinedForensicReportNotes())
                .withSummaryOfCircumstances(submitCpsServePtph.getSummaryOfCircumstances())
                .withSummaryOfCircumstancesNotes(submitCpsServePtph.getSummaryOfCircumstancesNotes())
                .withTag(submitCpsServePtph.getTag())
                .withThirdParty(submitCpsServePtph.getThirdParty())
                .withThirdPartyNotes(submitCpsServePtph.getThirdPartyNotes())
                .withTrialAdvocate(submitCpsServePtph.getTrialAdvocate())
                .withVictimPersonalStatement(submitCpsServePtph.getVictimPersonalStatement())
                .withVictimPersonalStatementNotes(submitCpsServePtph.getVictimPersonalStatementNotes())
                .withWitnesses(submitCpsServePtph.getWitnesses())
                .build();
    }

    private CpsServeCotrReceived convertSubmitCpsServeCotr(final SubmitCpsServeCotr submitCpsServeCotr) {
        return CpsServeCotrReceived.cpsServeCotrReceived()
                .withSubmissionId(submitCpsServeCotr.getSubmissionId())
                .withApplyForThePtrToBeVacated(submitCpsServeCotr.getApplyForThePtrToBeVacated())
                .withApplyForThePtrToBeVacatedDetails(submitCpsServeCotr.getApplyForThePtrToBeVacatedDetails())
                .withCertificationDate(submitCpsServeCotr.getCertificationDate())
                .withCertifyThatTheProsecutionIsTrialReady(submitCpsServeCotr.getCertifyThatTheProsecutionIsTrialReady())
                .withCertifyThatTheProsecutionIsTrialReadyDetails(submitCpsServeCotr.getCertifyThatTheProsecutionIsTrialReadyDetails())
                .withDefendantSubject(submitCpsServeCotr.getDefendantSubject())
                .withFormCompletedOnBehalfOfTheProsecutionBy(submitCpsServeCotr.getFormCompletedOnBehalfOfTheProsecutionBy())
                .withFurtherInformationToAssistTheCourt(submitCpsServeCotr.getFurtherInformationToAssistTheCourt())
                .withCertifyThatTheProsecutionIsTrialReadyDetails(submitCpsServeCotr.getCertifyThatTheProsecutionIsTrialReadyDetails())
                .withCertifyThatTheProsecutionIsTrialReady(submitCpsServeCotr.getCertifyThatTheProsecutionIsTrialReady())
                .withProsecutionCaseSubject(submitCpsServeCotr.getProsecutionCaseSubject())
                .withTrialDate(submitCpsServeCotr.getTrialDate())
                .withLastRecordedTimeEstimate(submitCpsServeCotr.getLastRecordedTimeEstimate())
                .withHasAllEvidenceToBeReliedOnBeenServed(submitCpsServeCotr.getHasAllEvidenceToBeReliedOnBeenServed())
                .withHasAllEvidenceToBeReliedOnBeenServedDetails(submitCpsServeCotr.getHasAllEvidenceToBeReliedOnBeenServedDetails())
                .withHasAllDisclosureBeenProvided(submitCpsServeCotr.getHasAllDisclosureBeenProvided())
                .withHasAllDisclosureBeenProvidedDetails(submitCpsServeCotr.getHasAllDisclosureBeenProvidedDetails())
                .withHaveOtherDirectionsBeenCompliedWith(submitCpsServeCotr.getHaveOtherDirectionsBeenCompliedWith())
                .withHaveOtherDirectionsBeenCompliedWithDetails(submitCpsServeCotr.getHaveOtherDirectionsBeenCompliedWithDetails())
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend(submitCpsServeCotr.getHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend())
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails(submitCpsServeCotr.getHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails())
                .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed(submitCpsServeCotr.getHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed())
                .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServedDetails(submitCpsServeCotr.getHaveAnyWitnessSummonsesRequiredBeenReceivedAndServedDetails())
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails(submitCpsServeCotr.getHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails())
                .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved(submitCpsServeCotr.getHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved())
                .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolvedDetails(submitCpsServeCotr.getHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolvedDetails())
                .withHaveInterpretersForWitnessesBeenArranged(submitCpsServeCotr.getHaveInterpretersForWitnessesBeenArranged())
                .withHaveInterpretersForWitnessesBeenArrangedDetails(submitCpsServeCotr.getHaveInterpretersForWitnessesBeenArrangedDetails())
                .withHaveEditedAbeInterviewsBeenPreparedAndAgreed(submitCpsServeCotr.getHaveEditedAbeInterviewsBeenPreparedAndAgreed())
                .withHaveEditedAbeInterviewsBeenPreparedAndAgreedDetails(submitCpsServeCotr.getHaveEditedAbeInterviewsBeenPreparedAndAgreedDetails())
                .withHaveInterpretersForWitnessesBeenArrangedDetails(submitCpsServeCotr.getHaveInterpretersForWitnessesBeenArrangedDetails())
                .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement(submitCpsServeCotr.getHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement())
                .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreementDetails(submitCpsServeCotr.getHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreementDetails())
                .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJury(submitCpsServeCotr.getIsTheCaseReadyToProceedWithoutDelayBeforeTheJury())
                .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJuryDetails(submitCpsServeCotr.getIsTheCaseReadyToProceedWithoutDelayBeforeTheJuryDetails())
                .withIsTheTimeEstimateCorrect(submitCpsServeCotr.getIsTheTimeEstimateCorrect())
                .withIsTheTimeEstimateCorrectDetails(submitCpsServeCotr.getIsTheTimeEstimateCorrectDetails())
                .build();
    }

    private CpsUpdateCotrReceived convertSubmitCpsUpdateCotr(SubmitCpsUpdateCotr submitCpsUpdateCotr) {
        return CpsUpdateCotrReceived.cpsUpdateCotrReceived()
                .withSubmissionId(submitCpsUpdateCotr.getSubmissionId())
                .withCotrId(submitCpsUpdateCotr.getCotrId())
                .withProsecutionCaseSubject(submitCpsUpdateCotr.getProsecutionCaseSubject())
                .withDefendantSubject(submitCpsUpdateCotr.getDefendantSubject())
                .withCertifyThatTheProsecutionIsTrialReady(submitCpsUpdateCotr.getCertifyThatTheProsecutionIsTrialReady())
                .withDate(submitCpsUpdateCotr.getDate())
                .withTrialDate(submitCpsUpdateCotr.getTrialDate())
                .withFormCompletedOnBehalfOfProsecutionBy(submitCpsUpdateCotr.getFormCompletedOnBehalfOfProsecutionBy())
                .withFurtherProsecutionInformationProvidedAfterCertification(submitCpsUpdateCotr.getFurtherProsecutionInformationProvidedAfterCertification())
                .build();
    }

    @Handles("stagingprosecutors.command.update-submission-status")
    public void updateSubmissionStatus(final Envelope<UpdateSubmissionStatus> updateSubmissionStatusEnvelope) throws EventStreamException {
        final UpdateSubmissionStatus updateSubmissionStatus = updateSubmissionStatusEnvelope.payload();
        LOGGER.info("stagingprosecutors.command.update-submission-status payload {}", updateSubmissionStatus);
        final EventStream eventStream = eventSource.getStreamById(updateSubmissionStatus.getSubmissionId());
        final CpsSubmission cpsSubmission = aggregateService.get(eventStream, CpsSubmission.class);

        final Stream<Object> events = cpsSubmission.updateSubmissionStatus(updateSubmissionStatus.getSubmissionId(), updateSubmissionStatus.getSubmissionStatus().toString(), updateSubmissionStatus.getErrors(), updateSubmissionStatus.getWarnings());
        appendEventsToStream(updateSubmissionStatusEnvelope, eventStream, events);
    }
    @SuppressWarnings("squid:S2221")
    @Handles("stagingprosecutors.command.receive-poca-email")
    public void receivePocaEmail(final Envelope<ReceivePocaEmail> pocaEmailEnvelope) throws FileServiceException, EventStreamException {
        boolean isParsed = true;
        final ReceivePocaEmail pocaEmail = pocaEmailEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(pocaEmail.getPocaMailId());
        final PocaEmailAggregate pocaEmailAggregate = aggregateService.get(eventStream, PocaEmailAggregate.class);
        Map<String, String> structuredData = new HashMap<>();

        final Optional<FileReference> fileReferenceOptional = fileRetriever.retrieve(pocaEmail.getPocaFileId());
        if(nonNull(fileReferenceOptional) && fileReferenceOptional.isPresent()){
            try(final FileReference fileReference = fileReferenceOptional.get()) {
                final InputStream docXInputStream = fileReference.getContentStream();
                if (null != docXInputStream) {
                    structuredData = DocxToCourtApplicationConverter.parse(docXInputStream);
                }
            } catch (Exception e) {
                LOGGER.error("Exception while retrieving file name", e);
                isParsed = false;
            }
        }

        final List<String> validateStructuredData = validateStructuredData(structuredData, isParsed);
        final Stream<Object> events;
        if (validateStructuredData.isEmpty()) {
            final CourtApplicationType courtApplicationType = referenceDataServiceImpl.retrieveApplicationTypes(structuredData.get("POCA-application-type"))
                    .orElseThrow(() -> new WebApplicationException("Application Type Can't Find"));

            final LocalDate nextBusinessDay = findNextBusinessDay(referenceDataServiceImpl.getPublicHolidays(LocalDate.now(), LocalDate.now().plusDays(30)));
            final SubmitApplication submitApplication = DocxToCourtApplicationConverter.prepareSubmitApplication(structuredData, courtApplicationType, nextBusinessDay);
            events = pocaEmailAggregate.pocaEmailValidated(pocaEmail.getPocaFileId(), pocaEmail.getPocaEmail(), pocaEmail.getEmailSubject(), submitApplication.getCourtApplication(), submitApplication.getBoxHearingRequest());
        } else {
            events = pocaEmailAggregate.pocaEmailNotValidated(pocaEmail.getPocaFileId(), pocaEmail.getPocaEmail(), pocaEmail.getEmailSubject(), validateStructuredData);
        }
        appendEventsToStream(pocaEmailEnvelope, eventStream, events);
    }

    private LocalDate findNextBusinessDay(final List<LocalDate> publicHolidays) {
        LocalDate day = LocalDate.now().plusDays(1);
        while (isWeekend(day) || publicHolidays.contains(day)) {
            day = day.plusDays(1);
        }
        return day;
    }

    public static boolean isWeekend(final LocalDate ld) {
        final DayOfWeek day = DayOfWeek.of(ld.get(ChronoField.DAY_OF_WEEK));
        return day == DayOfWeek.SUNDAY || day == DayOfWeek.SATURDAY;
    }

    private List<String> validateStructuredData(final Map<String, String> structuredData, final boolean isParsed) {
        final List<String> validateStructuredData = new ArrayList<>();
        if (structuredData.isEmpty() || !isParsed) {
            validateStructuredData.add("poca_application_cannot_be_read");
            return validateStructuredData;
        }
        populateValidateStructuredData(structuredData, validateStructuredData);
        return validateStructuredData;
    }

    private void populateValidateStructuredData(final Map<String, String> structuredData, final List<String> validateStructuredData) {
        if (!structuredData.isEmpty()) {
            if (!structuredData.containsKey("court-name")) {
                validateStructuredData.add("poca_court_location_missing");
            }
            if (!structuredData.containsKey("applicant-organisation-name") || !structuredData.containsKey("applicant-address-line1")) {
                validateStructuredData.add("poca_applicant_details_missing");
            }
            if((structuredData.containsKey("related-case-prosecutor-ou-code") != structuredData.containsKey("related-case-urn"))){
                validateStructuredData.add("missing_mandatory_fields");
            }
            validateRespondents(structuredData, validateStructuredData);
        }
    }

    private void validateRespondents(final Map<String, String> structuredDataMap, final List<String> validateStructuredData) {
        verifyRespondent("respondent-", structuredDataMap, validateStructuredData);
        verifyRespondent("respondent1-", structuredDataMap, validateStructuredData);
        verifyRespondent("respondent2-", structuredDataMap, validateStructuredData);
        verifyRespondent("respondent3-", structuredDataMap, validateStructuredData);
    }

    private void verifyRespondent(final String prefix, final Map<String, String> structuredDataMap, final List<String> validateStructuredData) {
        if (!getKeysByPatternMatching(structuredDataMap, prefix).isEmpty() &&
                !isPersonDetailsInvalid(structuredDataMap, prefix) &&
                !isOrganisationInvalid(structuredDataMap, prefix)) {
            validateStructuredData.add(POCA_RESPONDENT_DETAILS_MISSING);
        }
    }

    private boolean isPersonDetailsInvalid(final Map<String, String> structuredDataMap, final String prefix) {
        return structuredDataMap.containsKey(prefix + "first-name") &&
                structuredDataMap.containsKey(prefix + "last-name");
    }

    private boolean isOrganisationInvalid(final Map<String, String> structuredDataMap, final String prefix) {
        return structuredDataMap.containsKey(prefix + "organisation-name");
    }


    private Set<String> getKeysByPatternMatching(final Map<String, String> structuredDataMap, final String toBeMatched) {
        return structuredDataMap.keySet()
                .stream()
                .filter(s -> s.startsWith(toBeMatched))
                .collect(Collectors.toSet());
    }
}
