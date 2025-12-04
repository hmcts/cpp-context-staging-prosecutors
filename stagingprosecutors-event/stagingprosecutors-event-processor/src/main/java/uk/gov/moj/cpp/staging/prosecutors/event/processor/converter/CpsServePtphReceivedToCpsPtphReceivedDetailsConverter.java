package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePtphReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantOffencesSubjectsPtph;

import java.util.List;
import java.util.stream.Collectors;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsDefendant;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsPtphReceivedDetails;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ProsecutionCaseSubject;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus;

public class CpsServePtphReceivedToCpsPtphReceivedDetailsConverter implements Converter<CpsServePtphReceived, CpsPtphReceivedDetails> {

    @Override
    public CpsPtphReceivedDetails convert(final CpsServePtphReceived pthpReceived) {

        return CpsPtphReceivedDetails.cpsPtphReceivedDetails()
                .withSubmissionId(pthpReceived.getSubmissionId())
                .withProsecutionCaseSubject(ProsecutionCaseSubject.prosecutionCaseSubject()
                        .withUrn(pthpReceived.getProsecutionCaseSubject().getUrn())
                        .withProsecutingAuthority(pthpReceived.getProsecutionCaseSubject().getProsecutingAuthority()).build())
                .withCpsDefendant(buildCpsDefendantOffencesListFromCpsReceived(pthpReceived.getDefendantOffencesSubjects()))
                .withBadCharacter(pthpReceived.getBadCharacter())
                .withBadCharacterNotes(pthpReceived.getBadCharacterNotes())
                .withCctv(pthpReceived.getCctv())
                .withCctvNotes(pthpReceived.getCctvNotes())
                .withCpsOffice(pthpReceived.getCpsOffice())
                .withCriminalRecord(pthpReceived.getCriminalRecord())
                .withCriminalRecordNotes(pthpReceived.getCriminalRecordNotes())
                .withDisclosureManagementDoc(pthpReceived.getDisclosureManagementDoc())
                .withDisclosureManagementDocNotes(pthpReceived.getDisclosureManagementDocNotes())
                .withDraftIndictment(pthpReceived.getDraftIndictment())
                .withDraftIndictmentNotes(pthpReceived.getDraftIndictmentNotes())
                .withExhibitsForPAndICm(pthpReceived.getExhibitsForPAndICm())
                .withExhibitsForPAndICmNotes(pthpReceived.getExhibitsForPAndICmNotes())
                .withExpertEvidence(pthpReceived.getExpertEvidence())
                .withExpertEvidenceNotes(pthpReceived.getExpertEvidenceNotes())
                .withHearsay(pthpReceived.getHearsay())
                .withHearsayNotes(pthpReceived.getHearsayNotes())
                .withMedicalEvidence(pthpReceived.getMedicalEvidence())
                .withMedicalEvidenceNotes(pthpReceived.getMedicalEvidenceNotes())
                .withOfficerInTheCase(pthpReceived.getOfficerInTheCase())
                .withParticularsOfAnyFamily(pthpReceived.getParticularsOfAnyFamily())
                .withParticularsOfAnyRelatedCriminalProceedings(pthpReceived.getParticularsOfAnyRelatedCriminalProceedings())
                .withProsecutionCaseProgressionOfficer(pthpReceived.getProsecutionCaseProgressionOfficer())
                .withPtphAdvocate(pthpReceived.getPtphAdvocate())
                .withReviewDisclosableMaterial(pthpReceived.getReviewDisclosableMaterial())
                .withReviewDisclosableMaterialNotes(pthpReceived.getReviewDisclosableMaterialNotes())
                .withReviewingLawyer(pthpReceived.getReviewingLawyer())
                .withSpecialMeasures(pthpReceived.getSpecialMeasures())
                .withSpecialMeasuresNotes(pthpReceived.getSpecialMeasuresNotes())
                .withStatementsForPAndICm(pthpReceived.getStatementsForPAndICm())
                .withStatementsForPAndICmNotes(pthpReceived.getStatementsForPAndICmNotes())
                .withStreamlinedForensicReport(pthpReceived.getStreamlinedForensicReport())
                .withStreamlinedForensicReportNotes(pthpReceived.getStreamlinedForensicReportNotes())
                .withSummaryOfCircumstances(pthpReceived.getSummaryOfCircumstances())
                .withSummaryOfCircumstancesNotes(pthpReceived.getSummaryOfCircumstancesNotes())
                .withTag(pthpReceived.getTag())
                .withThirdParty(pthpReceived.getThirdParty())
                .withThirdPartyNotes(pthpReceived.getThirdPartyNotes())
                .withTrialAdvocate(pthpReceived.getTrialAdvocate())
                .withVictimPersonalStatement(pthpReceived.getVictimPersonalStatement())
                .withVictimPersonalStatementNotes(pthpReceived.getVictimPersonalStatementNotes())
                .withWitnesses(pthpReceived.getWitnesses())
                .withSubmissionStatus(SubmissionStatus.valueOf(pthpReceived.getSubmissionStatus().name()))
                .build();
    }

    private List<CpsDefendant> buildCpsDefendantOffencesListFromCpsReceived(final List<DefendantOffencesSubjectsPtph> receivedDefendantOffencesSubjects) {
        return receivedDefendantOffencesSubjects.stream()
                .map(defendantOffencesSubject -> new DefendantOffencesSubjectsPtphToCpsDefendantOffencesConverter().convert(defendantOffencesSubject))
                .collect(Collectors.toList());
    }
}
