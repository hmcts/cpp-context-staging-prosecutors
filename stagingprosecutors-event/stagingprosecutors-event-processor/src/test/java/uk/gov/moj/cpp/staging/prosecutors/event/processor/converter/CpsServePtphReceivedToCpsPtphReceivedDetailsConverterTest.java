package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePtphReceived;
import uk.gov.moj.cpp.staging.prosecutors.test.utils.FileResourceObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsDefendant;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsPtphReceivedDetails;
import org.junit.jupiter.api.Test;

public class CpsServePtphReceivedToCpsPtphReceivedDetailsConverterTest {

    private static final String PROSECUTOR_DEFENDANT_ID = "prosecutorDefendantId";
    private static final String CPS_DEFENDANT_ID = "cps-defendant-id";
    private static final String PROSECUTION_AUTHORITY = "The OU Code of the prosecuting authority";
    private static final String URN = "caseURN";
    public static final String PHONE = "8778888345";
    public static final String EMAIL = "test@test.com";
    private static final String NAME = "name";

    private final FileResourceObjectMapper fileResourceObjectMapper = new FileResourceObjectMapper();
    

    @Test
    public void shouldConvertCpsServePtphReceivedToCpsPtphReceivedDetails() throws IOException {
        final CpsServePtphReceived CpsServePtphReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-ptph-received.json", CpsServePtphReceived.class);
        final CpsPtphReceivedDetails CpsPtphReceivedDetails = new CpsServePtphReceivedToCpsPtphReceivedDetailsConverter().convert(CpsServePtphReceived);
        assertCpsPtphReceivedDetailsObject(CpsPtphReceivedDetails, CpsServePtphReceived.getSubmissionId());
    }

    private void assertCpsPtphReceivedDetailsObject(final CpsPtphReceivedDetails receivedDetails, final UUID submissionId) {
        assertThat(receivedDetails, notNullValue());
        final List<CpsDefendant> cpsDefendants = receivedDetails.getCpsDefendant();
        assertThat(cpsDefendants, hasSize(1));
        cpsDefendants.forEach(this::verifyCpsDefendantOffenceObject);

        assertThat(receivedDetails.getReviewingLawyer().getName(), is(NAME));
        assertThat(receivedDetails.getReviewingLawyer().getEmail(), is(EMAIL));
        assertThat(receivedDetails.getReviewingLawyer().getPhone(), is(PHONE));
        assertThat(receivedDetails.getPtphAdvocate().getName(), is(NAME));
        assertThat(receivedDetails.getPtphAdvocate().getEmail(), is(EMAIL));
        assertThat(receivedDetails.getPtphAdvocate().getPhone(), is(PHONE));
        assertThat(receivedDetails.getTrialAdvocate().getName(), is(NAME));
        assertThat(receivedDetails.getTrialAdvocate().getEmail(), is(EMAIL));
        assertThat(receivedDetails.getTrialAdvocate().getPhone(), is(PHONE));
        assertThat(receivedDetails.getOfficerInTheCase().getName(), is(NAME));
        assertThat(receivedDetails.getOfficerInTheCase().getEmail(), is(EMAIL));
        assertThat(receivedDetails.getOfficerInTheCase().getPhone(), is(PHONE));
        assertThat(receivedDetails.getProsecutionCaseProgressionOfficer().getName(), is(NAME));
        assertThat(receivedDetails.getProsecutionCaseProgressionOfficer().getEmail(), is(EMAIL));
        assertThat(receivedDetails.getProsecutionCaseProgressionOfficer().getPhone(), is(PHONE));

        assertThat(receivedDetails.getCpsOffice(), is("cps office"));
        assertThat(receivedDetails.getDraftIndictment(), is("N"));
        assertThat(receivedDetails.getDraftIndictmentNotes(), is("draftIndictmentNotes"));
        assertThat(receivedDetails.getSummaryOfCircumstances(), is("Y"));
        assertThat(receivedDetails.getSummaryOfCircumstancesNotes(), is("summaryOfCircumstancesNotes"));
        assertThat(receivedDetails.getStatementsForPAndICm(), is("Y"));
        assertThat(receivedDetails.getStatementsForPAndICmNotes(), is("statementsForPAndICmNotes"));
        assertThat(receivedDetails.getExhibitsForPAndICm(), is("N"));
        assertThat(receivedDetails.getExhibitsForPAndICmNotes(), is("exhibitsForPAndICmNotes"));
        assertThat(receivedDetails.getCctv(), is("Y"));
        assertThat(receivedDetails.getCctvNotes(), is("cctvNotes"));
        assertThat(receivedDetails.getStreamlinedForensicReport(), is("Y"));
        assertThat(receivedDetails.getStreamlinedForensicReportNotes(), is("streamlinedForensicReportNotes"));
        assertThat(receivedDetails.getMedicalEvidence(), is("Y"));
        assertThat(receivedDetails.getMedicalEvidenceNotes(), is("medicalEvidenceNotes"));
        assertThat(receivedDetails.getExpertEvidence(), is("Y"));
        assertThat(receivedDetails.getExpertEvidenceNotes(), is("ExpertEvidenceNotes"));
        assertThat(receivedDetails.getBadCharacter(), is("Y"));
        assertThat(receivedDetails.getBadCharacterNotes(), is("badCharacterNotes"));
        assertThat(receivedDetails.getHearsay(), is("Y"));
        assertThat(receivedDetails.getHearsayNotes(), is("hearsayNotes"));
        assertThat(receivedDetails.getCriminalRecord(), is("Y"));
        assertThat(receivedDetails.getCriminalRecordNotes(), is("criminalRecordNotes"));
        assertThat(receivedDetails.getVictimPersonalStatement(), is("NA"));
        assertThat(receivedDetails.getVictimPersonalStatementNotes(), is("victimPersonalStatementNotes"));
        assertThat(receivedDetails.getDisclosureManagementDoc(), is("Y"));
        assertThat(receivedDetails.getDisclosureManagementDocNotes(), is("disclosureManagementDocNotes"));
        assertThat(receivedDetails.getThirdParty(), is("Y"));
        assertThat(receivedDetails.getThirdPartyNotes(), is("thirdPartyNotes"));


        assertThat(receivedDetails.getProsecutionCaseSubject(), notNullValue());
        assertThat(receivedDetails.getProsecutionCaseSubject().getProsecutingAuthority(), is(PROSECUTION_AUTHORITY));
        assertThat(receivedDetails.getProsecutionCaseSubject().getUrn(), is(URN));
        assertThat(receivedDetails.getSubmissionId(), is(submissionId));
        assertThat(receivedDetails.getSubmissionStatus().toString(), is(PENDING.toString()));
    }

    private void verifyCpsDefendantOffenceObject(final CpsDefendant cpsDefendant) {
        assertThat(cpsDefendant.getProsecutorDefendantId(), is(PROSECUTOR_DEFENDANT_ID));
        assertThat(cpsDefendant.getCpsDefendantId(), is(CPS_DEFENDANT_ID));
        assertThat(cpsDefendant.getMatchingId(), notNullValue());
    }
}
