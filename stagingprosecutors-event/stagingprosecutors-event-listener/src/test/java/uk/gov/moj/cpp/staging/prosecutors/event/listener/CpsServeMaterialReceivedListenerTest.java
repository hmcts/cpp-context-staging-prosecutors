package uk.gov.moj.cpp.staging.prosecutors.event.listener;

import static com.google.common.collect.ImmutableList.of;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeBcmReceived.cpsServeBcmReceived;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeCotrReceived.cpsServeCotrReceived;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived.cpsServePetReceived;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePtphReceived.cpsServePtphReceived;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsUpdateCotrReceived.cpsUpdateCotrReceived;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.FAILED;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;
import static uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType.BCM;
import static uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType.COTR;
import static uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType.PET;
import static uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType.PTPH;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsProsecutionCaseSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsProsecutionCaseSubjectPtph;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeBcmReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePtphReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsUpdateCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantOffencesSubjects;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantOffencesSubjectsPtph;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProblemValue;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionWillRelyOn;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatusUpdated;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.SubmissionRepository;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CpsServeMaterialReceivedListenerTest {

    private static final String CPS_SERVE_PET_RECEIVED = "stagingprosecutors.event.cps-serve-pet-received";
    private static final String CPS_SERVE_BCM_RECEIVED = "stagingprosecutors.event.cps-serve-bcm-received";
    private static final String CPS_SERVE_PTPH_RECEIVED = "stagingprosecutors.event.cps-serve-ptph-received";
    private static final String CPS_SERVE_COTR_RECEIVED = "stagingprosecutors.event.cps-serve-cotr-received";
    private static final String CPS_UPDATE_COTR_RECEIVED = "stagingprosecutors.event.cps-update-cotr-received";

    private static final String NO = "N";
    private static final String caseUrn = "TVL1234";
    private final UUID submissionId = randomUUID();
    @Mock
    private SubmissionRepository submissionRepository;
    @InjectMocks
    private CpsServeMaterialReceivedListener cpsServeMaterialReceivedListener;
    @Captor
    private ArgumentCaptor<Submission> argumentCaptor;
    @Mock
    private ObjectToJsonObjectConverter converter;


    @Test
    public void shouldHandleCpsPetRequestReceived() {
        final List<ProsecutionWillRelyOn> prosecutionWillRelyOn = new ArrayList<>();

        prosecutionWillRelyOn.add(ProsecutionWillRelyOn.CCTV_EVIDENCE);

        final CpsServePetReceived cpsServePetReceived =
                cpsServePetReceived().withSubmissionId(submissionId)
                        .withSubmissionStatus(PENDING)
                        .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
                        .withDefendantOffencesSubjects(createDefendantOffencesSubjects())
                        .withAreThereAnyPendingEnquiriesOrLinesOfInvestigation(NO)
                        .withCourtToArrangeADiscussionOfGroundRulesForQuestioning(NO)
                        .withDoesTheProsecutorIntendToServeMoreEvidence(NO)
                        .withHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith(NO)
                        .withHasDefendantHasBeenAVictimOfSlaveryOrExploitation(NO)
                        .withVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirection(NO)
                        .withExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFact(NO)
                        .withProsecutionWillRelyOn(prosecutionWillRelyOn)
                        .withWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoom(NO)
                        .withParentGuardianToAttend(false)
                        .build();

        final Envelope<CpsServePetReceived> envelope = newEnvelope(CPS_SERVE_PET_RECEIVED, cpsServePetReceived);

        cpsServeMaterialReceivedListener.cpsServePetReceived(envelope);
        verify(submissionRepository).save(argumentCaptor.capture());

        final Submission submission = argumentCaptor.getValue();
        verifyCpsMaterialReceived(submission, PET);
    }

    @Test
    public void shouldHandleCpsBcmRequestReceived() {
        final CpsServeBcmReceived cpsServeBcmReceived =
                cpsServeBcmReceived().withSubmissionId(submissionId)
                        .withSubmissionStatus(PENDING)
                        .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
                        .withDefendantOffencesSubject(createDefendantOffencesSubjects())
                        .withEvidencePrePTPH("Evidence")
                        .build();

        final Envelope<CpsServeBcmReceived> envelope = newEnvelope(CPS_SERVE_BCM_RECEIVED, cpsServeBcmReceived);

        cpsServeMaterialReceivedListener.cpsServeBcmReceived(envelope);
        verify(submissionRepository).save(argumentCaptor.capture());

        final Submission submission = argumentCaptor.getValue();
        verifyCpsMaterialReceived(submission, BCM);
    }

    private void verifyCpsMaterialReceived(final Submission submission, final SubmissionType submissionType) {
        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(PENDING.toString()));
        assertThat(submission.getType(), is(submissionType));
        assertThat(submission.getCaseUrn(), is(caseUrn));
    }

    @Test
    public void shouldHandleCpsPtphRequestReceived() {

        final List<DefendantOffencesSubjectsPtph> defendantOffencesSubjectsPtph = new ArrayList<>();

        defendantOffencesSubjectsPtph.add(DefendantOffencesSubjectsPtph.defendantOffencesSubjectsPtph()
                .withDefendant(DefendantSubject.defendantSubject()
                        .withProsecutorDefendantId("PDEFID001")
                        .build())
                .withPrincipalCharges("charges")
                .build());

        final CpsServePtphReceived cpsServePtphReceived =
                cpsServePtphReceived().withSubmissionId(submissionId)
                        .withSubmissionStatus(PENDING)
                        .withProsecutionCaseSubject(createCpsProsecutionCaseSubjectPtph())
                        .withDefendantOffencesSubjects(defendantOffencesSubjectsPtph)
                        .build();

        final Envelope<CpsServePtphReceived> envelope = newEnvelope(CPS_SERVE_PTPH_RECEIVED, cpsServePtphReceived);

        cpsServeMaterialReceivedListener.cpsServePtphReceived(envelope);

        verify(submissionRepository).save(argumentCaptor.capture());

        final Submission submission = argumentCaptor.getValue();

        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(PENDING.toString()));
        assertThat(submission.getType(), is(PTPH));
        assertThat(submission.getCaseUrn(), is(caseUrn));
    }

    @Test
    public void shouldHandleCpsUpdateCotrRequestReceived() {

        final List<DefendantSubject> defendantSubjects = new ArrayList<>();
        defendantSubjects.add(DefendantSubject.defendantSubject()
                .withProsecutorDefendantId("prosecutor defendant id")
                .build());

        final CpsUpdateCotrReceived cpsUpdateCotrReceived =
                cpsUpdateCotrReceived().withSubmissionId(submissionId)
                        .withSubmissionStatus(PENDING)
                        .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
                        .withDefendantSubject(defendantSubjects)
                        .build();

        final Envelope<CpsUpdateCotrReceived> envelope = newEnvelope(CPS_UPDATE_COTR_RECEIVED, cpsUpdateCotrReceived);

        cpsServeMaterialReceivedListener.cpsUpdateCotrReceived(envelope);

        verify(submissionRepository).save(argumentCaptor.capture());

        final Submission submission = argumentCaptor.getValue();

        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(PENDING.toString()));
        assertThat(submission.getType(), is(COTR));
        assertThat(submission.getCaseUrn(), is(caseUrn));
    }

    @Test
    public void shouldHandleCpsCotrRequestReceived() {
        final List<ProsecutionWillRelyOn> prosecutionWillRelyOn = new ArrayList<>();

        prosecutionWillRelyOn.add(ProsecutionWillRelyOn.CCTV_EVIDENCE);

        final List<DefendantSubject> defendantSubjects = new ArrayList<>();
        defendantSubjects.add(DefendantSubject.defendantSubject()
                .withProsecutorDefendantId("prosecutor defendant id")
                .build());

        final CpsServeCotrReceived cpsServeCotrReceived =
                cpsServeCotrReceived().withSubmissionId(submissionId)
                        .withSubmissionStatus(PENDING)
                        .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
                        .withDefendantSubject(defendantSubjects)
                        .build();

        final Envelope<CpsServeCotrReceived> envelope = newEnvelope(CPS_SERVE_COTR_RECEIVED, cpsServeCotrReceived);

        cpsServeMaterialReceivedListener.cpsServeCotrReceived(envelope);
        verify(submissionRepository).save(argumentCaptor.capture());

        final Submission submission = argumentCaptor.getValue();
        verifyCpsMaterialReceived(submission, COTR);
    }

    @Test
    public void shouldHandleSubmissionStatusUpdated(){
        final Problem error = newError("CASE_URN_NOT_FOUND", "Expired", "Submission has been in a pending state for 28 days");

        final SubmissionStatusUpdated submissionStatusUpdated = SubmissionStatusUpdated.submissionStatusUpdated()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(SubmissionStatus.FAILED)
                .withErrors(of(error))
                .build();

        final Submission savedSubmission = mock(Submission.class);
        final Envelope<SubmissionStatusUpdated> envelope = newEnvelope("stagingprosecutors.event.submission-status-updated", submissionStatusUpdated);
        final JsonObject errorAsJson = errorAsJson(error.getValues().get(0));

        when(submissionRepository.findBy(submissionId)).thenReturn(savedSubmission);
        when(converter.convert(error)).thenReturn(errorAsJson);

        cpsServeMaterialReceivedListener.submissionStatusUpdated(envelope);

        verify(submissionRepository).findBy(submissionId);
        verify(savedSubmission).setSubmissionStatus(FAILED.toString());
        verify(savedSubmission).setErrors(createArrayBuilder().add(errorAsJson).build());
        verify(savedSubmission).setCompletedAt(envelope.metadata().createdAt().get());
    }

    private <T> Envelope<T> newEnvelope(final String name, final T payload) {
        return envelopeFrom(metadataWithRandomUUID(name).createdAt(ZonedDateTime.now(UTC)), payload);
    }

    private List<DefendantOffencesSubjects> createDefendantOffencesSubjects() {
        final List<DefendantOffencesSubjects> defendantOffencesSubjects = new ArrayList<>();
        final List<OffenceSubject> offence = new ArrayList<>();

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

        return defendantOffencesSubjects;
    }

    private CpsProsecutionCaseSubject createCpsProsecutionCaseSubject() {
        return CpsProsecutionCaseSubject.cpsProsecutionCaseSubject()
                .withUrn(caseUrn)
                .withProsecutingAuthority("OUCODE")
                .build();
    }

    private CpsProsecutionCaseSubjectPtph createCpsProsecutionCaseSubjectPtph() {
        return CpsProsecutionCaseSubjectPtph.cpsProsecutionCaseSubjectPtph()
                .withUrn(caseUrn)
                .withProsecutingAuthority("OUCODE")
                .build();
    }

    private static Problem newError(final String code, final String key, final String value) {
        return Problem.problem()
                .withCode(code)
                .withValues(ImmutableList.of(
                        ProblemValue
                                .problemValue()
                                .withKey(key)
                                .withValue(value)
                                .build()))
                .build();
    }

    private static JsonObject errorAsJson(final ProblemValue problemValue) {
        return createObjectBuilder()
                .add("key", problemValue.getKey())
                .add("value", problemValue.getValue())
                .build();
    }
}
