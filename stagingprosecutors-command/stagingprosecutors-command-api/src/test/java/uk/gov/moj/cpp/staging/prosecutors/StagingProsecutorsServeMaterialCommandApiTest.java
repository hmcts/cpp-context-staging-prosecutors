package uk.gov.moj.cpp.staging.prosecutors;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.YesNo.N;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.YesNoNa.Y;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.command.api.AreThereanypendingEnquiriesorLinesOfInvestigation;
import uk.gov.moj.cpp.staging.prosecutors.command.api.CertifyThatTheProsecutionIsTrialReady;
import uk.gov.moj.cpp.staging.prosecutors.command.api.CourtToArrangeADiscussionOfGroundRulesForQuestioning;
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
import uk.gov.moj.cpp.staging.prosecutors.command.api.DoesTheProsecutorIntendToServeMoreEvidence;
import uk.gov.moj.cpp.staging.prosecutors.command.api.ExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFact;
import uk.gov.moj.cpp.staging.prosecutors.command.api.HasDefendantHasBeenAVictimOfSlaveryOrExploitation;
import uk.gov.moj.cpp.staging.prosecutors.command.api.HasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith;
import uk.gov.moj.cpp.staging.prosecutors.command.api.VaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirection;
import uk.gov.moj.cpp.staging.prosecutors.command.api.WillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoom;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsProsecutionCaseSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsProsecutionCaseSubjectPtph;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantOffencesSubjects;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantOffencesSubjectsPtph;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionWillRelyOn;
import uk.gov.moj.cpp.staging.prosecutors.uuid.UUIDProducer;
import uk.gov.moj.cpp.staging.prosecutors.validators.BcmValidator;
import uk.gov.moj.cpp.staging.prosecutors.validators.CotrValidator;
import uk.gov.moj.cpp.staging.prosecutors.validators.PetValidator;
import uk.gov.moj.cpp.staging.prosecutors.validators.PtphValidator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsServeMaterialCommandApiTest {

    private static final UUID SUBMISSION_ID = randomUUID();
    private static final String PROSECUTOR_DEFENDANT_ID = "PDEFID001";
    private static final String CJS_OFFENCE_CODE = "CJSCODE001";
    private static final String OFFENCE_DATE = "2021-09-27";
    private static final String OFFENCE_WORDING = "Test Offence Wording";
    private static final String CPS_SERVE_PET = "stagingprosecutors.cps-serve-pet";
    private static final String CPS_SERVE_BCM = "stagingprosecutors.cps-serve-bcm";
    private static final String CPS_SERVE_PTPH = "stagingprosecutors.cps-serve-ptph";
    private static final String CPS_SERVE_COTR = "stagingprosecutors.cps-serve-cotr";
    private static final String COMMAND_CPS_SERVE_PET = "stagingprosecutors.command.submit-cps-serve-pet";
    private static final String COMMAND_CPS_SERVE_BCM = "stagingprosecutors.command.submit-cps-serve-bcm";
    private static final String COMMAND_CPS_SERVE_PTPH = "stagingprosecutors.command.submit-cps-serve-ptph";
    private static final String COMMAND_CPS_SERVE_COTR = "stagingprosecutors.command.submit-cps-serve-cotr";
    private static final String CPS_UPDATE_COTR = "stagingprosecutors.cps-update-cotr";
    private static final String COMMAND_CPS_UPDATE_COTR = "stagingprosecutors.command.submit-cps-update-cotr";
    private static final String TAG_TEXT = "tag text";
    private static final String EVIDENCE_PRE_PTPH_TEXT = "evidencePrePTPH text";
    private static final String EVIDENCE_POST_PTPH_TEXT = "evidencePostPTPH text";
    private static final String OTHER_INFORMATION_TEXT = "otherInformation text";
    private static final UUID UPDATE_SUBMISSION_ID = randomUUID();
    private static final String ADDITIONAL_INFO = "Additional Info";

    @InjectMocks
    private StagingProsecutorsServeMaterialCommandApi stagingProsecutorsServeMaterialCommandApi;

    @Mock
    private PetValidator petValidator;
    @Mock
    private BcmValidator bcmValidator;
    @Mock
    private CotrValidator cotrValidator;
    @Mock
    private PtphValidator ptphValidator;
    @Mock
    private Sender sender;
    @Mock
    private UUIDProducer uuidProducer;

    @Captor
    private ArgumentCaptor<Envelope<CpsServePetWithSubmissionId>> cpsServePetEnvelopeCaptor;
    @Captor
    private ArgumentCaptor<Envelope<CpsServeBcmWithSubmissionId>> cpsServeBcmEnvelopeCaptor;
    @Captor
    private ArgumentCaptor<Envelope<CpsServePtphWithSubmissionId>> cpsServePtphEnvelopeCaptor;
    @Captor
    private ArgumentCaptor<Envelope<CpsServeCotrWithSubmissionId>> cpsServeCotrEnvelopeCaptor;
    @Captor
    private ArgumentCaptor<Envelope<CpsUpdateCotrWithSubmissionId>> cpsUpdateCotrEnvelopeCaptor;


    @BeforeEach
    public void setup() {
        stagingProsecutorsServeMaterialCommandApi.baseResponseURL = "test-base-url/";
        when(uuidProducer.generateUUID()).thenReturn(SUBMISSION_ID);
    }

    @Test
    public void handleCpsServePet() {
        final CpsServePet cpsServePetPayload = createCpsServePetPayload();

        final Envelope<CpsServePet> originalEnvelope = createCpsServePetEnvelope(cpsServePetPayload);

        final Envelope<UrlResponse> stagingProsecutorsResponseEnvelope = stagingProsecutorsServeMaterialCommandApi.cpsServePet(originalEnvelope);

        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertThat(stagingProsecutorsResponseEnvelope.payload().getStatusURL(), equalTo(expectedStatusURL));
        assertThat(stagingProsecutorsResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));

        verify(sender).send(cpsServePetEnvelopeCaptor.capture());

        final Envelope<CpsServePetWithSubmissionId> sentEnvelope = cpsServePetEnvelopeCaptor.getValue();
        final Metadata sentEnvelopeMetadata = sentEnvelope.metadata();
        final CpsServePetWithSubmissionId sentEnvelopePayload = sentEnvelope.payload();

        assertThat(sentEnvelopeMetadata.name(), equalTo(COMMAND_CPS_SERVE_PET));
        assertThat(sentEnvelopePayload.getAdditionalInformation(), is(ADDITIONAL_INFO));
    }

    @Test
    public void handleCpsServeCotr() {
        final CpsServeCotr cpsServeCotrPayload = createCpsServeCotrPayload();

        final Envelope<CpsServeCotr> originalEnvelope = createCpsServeCotrEnvelope(cpsServeCotrPayload);

        final Envelope<UrlResponse> stagingProsecutorsResponseEnvelope = stagingProsecutorsServeMaterialCommandApi.cpsServeCotr(originalEnvelope);

        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertThat(stagingProsecutorsResponseEnvelope.payload().getStatusURL(), equalTo(expectedStatusURL));
        assertThat(stagingProsecutorsResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));

        verify(sender).send(cpsServeCotrEnvelopeCaptor.capture());

        final Envelope<CpsServeCotrWithSubmissionId> sentEnvelope = cpsServeCotrEnvelopeCaptor.getValue();
        final Metadata sentEnvelopeMetadata = sentEnvelope.metadata();
        final CpsServeCotrWithSubmissionId sentEnvelopePayload = sentEnvelope.payload();

        assertThat(sentEnvelopeMetadata.name(), equalTo(COMMAND_CPS_SERVE_COTR));
    }

    @Test
    public void handleCpsServeBcm() {
        final CpsServeBcm cpsServeBcmPayload = createCpsServeBcmPayload();

        final Envelope<CpsServeBcm> originalEnvelope = createCpsServeBcmEnvelope(cpsServeBcmPayload);

        final Envelope<UrlResponse> stagingProsecutorsResponseEnvelope = stagingProsecutorsServeMaterialCommandApi.cpsServeBcm(originalEnvelope);

        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertEquals(expectedStatusURL, stagingProsecutorsResponseEnvelope.payload().getStatusURL());
        assertEquals(SUBMISSION_ID, stagingProsecutorsResponseEnvelope.payload().getSubmissionId());

        verify(sender).send(cpsServeBcmEnvelopeCaptor.capture());

        final Envelope<CpsServeBcmWithSubmissionId> sentEnvelope = cpsServeBcmEnvelopeCaptor.getValue();
        final Metadata sentEnvelopeMetadata = sentEnvelope.metadata();
        final CpsServeBcmWithSubmissionId sentEnvelopePayload = sentEnvelope.payload();
        final DefendantOffencesSubjects defendantOffencesSubjects = sentEnvelopePayload.getDefendantOffencesSubject().get(0);
        final DefendantSubject defendantSubject = defendantOffencesSubjects.getDefendant();
        final OffenceSubject offenceSubject = defendantOffencesSubjects.getOffences().get(0);

        assertEquals(TAG_TEXT, sentEnvelopePayload.getTag());
        assertEquals(EVIDENCE_PRE_PTPH_TEXT, sentEnvelopePayload.getEvidencePrePTPH());
        assertEquals(COMMAND_CPS_SERVE_BCM, sentEnvelopeMetadata.name());
        assertEquals(PROSECUTOR_DEFENDANT_ID, defendantSubject.getProsecutorDefendantId());
        assertEquals(CJS_OFFENCE_CODE, offenceSubject.getCjsOffenceCode());
        assertEquals(OFFENCE_DATE, offenceSubject.getOffenceDate());
        assertEquals(OFFENCE_WORDING, offenceSubject.getOffenceWording());
    }

    @Test
    public void handleCpsServePtph() {
        final CpsServePtph cpsServePtphPayload = createCpsServePtphPayload();

        final Envelope<CpsServePtph> originalEnvelope = createCpsServePtphEnvelope(cpsServePtphPayload);

        final Envelope<UrlResponse> stagingProsecutorsResponseEnvelope = stagingProsecutorsServeMaterialCommandApi.cpsServePtph(originalEnvelope);

        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertEquals(expectedStatusURL, stagingProsecutorsResponseEnvelope.payload().getStatusURL());
        assertEquals(SUBMISSION_ID, stagingProsecutorsResponseEnvelope.payload().getSubmissionId());

        verify(sender).send(cpsServePtphEnvelopeCaptor.capture());

        final Envelope<CpsServePtphWithSubmissionId> sentEnvelope = cpsServePtphEnvelopeCaptor.getValue();
        final Metadata sentEnvelopeMetadata = sentEnvelope.metadata();
        final CpsServePtphWithSubmissionId sentEnvelopePayload = sentEnvelope.payload();
        final DefendantOffencesSubjectsPtph defendantOffencesSubjectsPtph = sentEnvelopePayload.getDefendantOffencesSubjects().get(0);
        final DefendantSubject defendantSubject = defendantOffencesSubjectsPtph.getDefendant();
        final String principalCharges = defendantOffencesSubjectsPtph.getPrincipalCharges();

        assertEquals(COMMAND_CPS_SERVE_PTPH, sentEnvelopeMetadata.name());
        assertEquals(PROSECUTOR_DEFENDANT_ID, defendantSubject.getProsecutorDefendantId());
        assertEquals("principalCharges", principalCharges);
    }

    @Test
    public void handleCpsUpdateCotr() {
        CpsUpdateCotr cpsUpdateCotrPayload = createCpsUpdateCotrPayload();

        final Envelope<CpsUpdateCotr> cpsUpdateCotrEnvelope = createCpsUpdateCotrEnvelope(cpsUpdateCotrPayload);

        final Envelope<UrlResponse> stagingProsecutorsResponseEnvelope = stagingProsecutorsServeMaterialCommandApi.cpsUpdateCotr(cpsUpdateCotrEnvelope);

        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertEquals(expectedStatusURL, stagingProsecutorsResponseEnvelope.payload().getStatusURL());
        assertEquals(SUBMISSION_ID, stagingProsecutorsResponseEnvelope.payload().getSubmissionId());

        verify(sender).send(cpsUpdateCotrEnvelopeCaptor.capture());

        final Envelope<CpsUpdateCotrWithSubmissionId> sentEnvelope = cpsUpdateCotrEnvelopeCaptor.getValue();
        final Metadata sentEnvelopeMetadata = sentEnvelope.metadata();
        final CpsUpdateCotrWithSubmissionId sentEnvelopePayload = sentEnvelope.payload();
        final DefendantSubject defendantSubject = sentEnvelopePayload.getDefendantSubject().get(0);
        assertEquals(LocalDate.now().plusDays(2),sentEnvelopePayload.getTrialDate());

        assertEquals(COMMAND_CPS_UPDATE_COTR, sentEnvelopeMetadata.name());
        assertEquals(PROSECUTOR_DEFENDANT_ID, defendantSubject.getProsecutorDefendantId());
        assertEquals(UPDATE_SUBMISSION_ID, sentEnvelopePayload.getCotrId());
    }

    private Envelope<CpsServePet> createCpsServePetEnvelope(final CpsServePet payload) {
        final Metadata metadata = Envelope.metadataBuilder().withId(randomUUID())
                .withName(CPS_SERVE_PET)
                .createdAt(now()).build();

        return Envelope.envelopeFrom(metadata, payload);
    }


    private List<DefendantOffencesSubjects> createDefendantOffencesSubjects() {
        final List<DefendantOffencesSubjects> defendantOffencesSubjects = new ArrayList<>();
        final List<OffenceSubject> offence = new ArrayList<>();

        offence.add(OffenceSubject.offenceSubject()
                .withCjsOffenceCode(CJS_OFFENCE_CODE)
                .withOffenceDate(OFFENCE_DATE)
                .withOffenceWording(OFFENCE_WORDING)
                .build());

        defendantOffencesSubjects.add(DefendantOffencesSubjects.defendantOffencesSubjects()
                .withDefendant(DefendantSubject.defendantSubject()
                        .withProsecutorDefendantId(PROSECUTOR_DEFENDANT_ID)
                        .build())
                .withOffences(offence)
                .build());

        return defendantOffencesSubjects;
    }

    private CpsProsecutionCaseSubject createCpsProsecutionCaseSubject() {
        return CpsProsecutionCaseSubject.cpsProsecutionCaseSubject()
                .withUrn("TEST001")
                .withProsecutingAuthority("OUCODE11111")
                .build();
    }

    private CpsProsecutionCaseSubjectPtph createCpsProsecutionCaseSubjectPtph() {
        return CpsProsecutionCaseSubjectPtph.cpsProsecutionCaseSubjectPtph()
                .withUrn("TEST001")
                .withProsecutingAuthority("OUCODE")
                .build();
    }

    private CpsServePet createCpsServePetPayload() {
        final List<DefendantOffencesSubjects> defendantOffencesSubjects = createDefendantOffencesSubjects();
        final List<ProsecutionWillRelyOn> prosecutionWillRelyOn = new ArrayList<>();

        prosecutionWillRelyOn.add(ProsecutionWillRelyOn.CCTV_EVIDENCE);

        final CpsServePet payload = CpsServePet.cpsServePet()
                .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
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
                .withParentGuardianToAttend(false)
                .withAdditionalInformation(ADDITIONAL_INFO)
                .withIsYouth(true)
                .build();

        return payload;
    }

    private CpsServeBcm createCpsServeBcmPayload() {
        final List<DefendantOffencesSubjects> defendantOffencesSubjects = createDefendantOffencesSubjects();

        final CpsServeBcm payload = CpsServeBcm.cpsServeBcm()
                .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
                .withDefendantOffencesSubject(defendantOffencesSubjects)
                .withTag(TAG_TEXT)
                .withEvidencePrePTPH(EVIDENCE_PRE_PTPH_TEXT)
                .build();

        return payload;
    }

    private Envelope<CpsServeBcm> createCpsServeBcmEnvelope(final CpsServeBcm payload) {
        final Metadata metadata = Envelope.metadataBuilder().withId(randomUUID())
                .withName(CPS_SERVE_BCM)
                .createdAt(now()).build();

        return Envelope.envelopeFrom(metadata, payload);
    }

    private CpsServePtph createCpsServePtphPayload() {
        final List<DefendantOffencesSubjectsPtph> defendantOffencesSubjects = new ArrayList<>();
        defendantOffencesSubjects.add(DefendantOffencesSubjectsPtph.defendantOffencesSubjectsPtph()
                        .withDefendant(DefendantSubject.defendantSubject()
                                .withProsecutorDefendantId(PROSECUTOR_DEFENDANT_ID)
                                .build())
                .withPrincipalCharges("principalCharges")
                .build());

        final CpsServePtph payload = CpsServePtph.cpsServePtph()
                .withProsecutionCaseSubject(createCpsProsecutionCaseSubjectPtph())
                .withDefendantOffencesSubjects(defendantOffencesSubjects)
                .build();

        return payload;
    }

    private Envelope<CpsServePtph> createCpsServePtphEnvelope(final CpsServePtph payload) {
        final Metadata metadata = Envelope.metadataBuilder().withId(randomUUID())
                .withName(CPS_SERVE_PTPH)
                .createdAt(now()).build();

        return Envelope.envelopeFrom(metadata, payload);
    }

    private CpsUpdateCotr createCpsUpdateCotrPayload() {
        List<DefendantSubject> defendantSubjects = new ArrayList<>();
        defendantSubjects.add(DefendantSubject.defendantSubject()
        .withProsecutorDefendantId("PDEFID001")
        .build());

        final CpsUpdateCotr payload = CpsUpdateCotr.cpsUpdateCotr()
                .withCotrSubmissionId(UPDATE_SUBMISSION_ID)
                .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
                .withDefendantSubject(defendantSubjects)
                .withCertifyThatTheProsecutionIsTrialReady(CertifyThatTheProsecutionIsTrialReady.Y)
                .withDate(LocalDate.now())
                .withTrialDate(LocalDate.now().plusDays(2))
                .withFormCompletedOnBehalfOfProsecutionBy("Form Completed On Behalf Of Prosecution By")
                .withFurtherProsecutionInformationProvidedAfterCertification("Further Prosecution Information Provided After Certification")
                .build();

        return payload;
    }

    private Envelope<CpsUpdateCotr> createCpsUpdateCotrEnvelope(final CpsUpdateCotr payload) {
        final Metadata metadata = Envelope.metadataBuilder().withId(randomUUID())
                .withName(CPS_UPDATE_COTR)
                .createdAt(now()).build();

        return Envelope.envelopeFrom(metadata, payload);
    }

    private Envelope<CpsServeCotr> createCpsServeCotrEnvelope(final CpsServeCotr payload) {
        final Metadata metadata = Envelope.metadataBuilder().withId(randomUUID())
                .withName(CPS_SERVE_COTR)
                .createdAt(now()).build();

        return Envelope.envelopeFrom(metadata, payload);
    }


    private CpsServeCotr createCpsServeCotrPayload() {
        final List<DefendantSubject> defendantSubjects = new ArrayList<>();
        defendantSubjects.add(DefendantSubject.defendantSubject()
            .withProsecutorDefendantId("prosecutor defendant id")
            .build());
        final List<ProsecutionWillRelyOn> prosecutionWillRelyOn = new ArrayList<>();

        prosecutionWillRelyOn.add(ProsecutionWillRelyOn.CCTV_EVIDENCE);

        final CpsServeCotr payload = CpsServeCotr.cpsServeCotr()
                .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
                .withDefendantSubject(defendantSubjects)
                .withApplyForThePtrToBeVacated(Y)
                .withHasAllEvidenceToBeReliedOnBeenServed(N)
                .withHasAllDisclosureBeenProvided(N)
                .withHaveOtherDirectionsBeenCompliedWith(N)
                .withHaveInterpretersForWitnessesBeenArranged(Y)
                .withHaveEditedAbeInterviewsBeenPreparedAndAgreed(Y)
                .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement(Y)
                .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJury(N)
                .withIsTheTimeEstimateCorrect(N)
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend(N)
                .withCertifyThatTheProsecutionIsTrialReady(N)
                .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed(Y)
                .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved(Y)
                .build();

        return payload;
    }
}
