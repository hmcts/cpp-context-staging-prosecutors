package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeBcmReceived.cpsServeBcmReceived;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived.cpsServePetReceived;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.DateUtil;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsCaseContact;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsPersonDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsProsecutionCaseSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeBcmReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsUpdateCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantOffencesSubjects;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantParentGuardian;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.LocalAuthorityDetailsForYouthDefendants;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionWillRelyOn;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutorPersonDefendantDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ApplicationsForDirectionsGroup;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsCotrReceivedDetails;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsDefendantOffences;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsPetReceivedDetails;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsUpdateCotrReceivedDetails;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.Defence;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.PetFormData;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.Prosecution;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ProsecutorGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CpsServeMaterialReceivedProcessorTest {

    public static final String CPS_DEFENDANT_ID = "af4bc5bb-9393-462a-9673-e40ac2218552";
    private static final String CPS_SERVE_PET_RECEIVED_EVENT = "stagingprosecutors.event.cps-serve-pet-received";
    private static final String CPS_SERVE_COTR_RECEIVED_EVENT = "stagingprosecutors.event.cps-serve-cotr-received";
    private static final String CPS_UPDATE_COTR_RECEIVED_EVENT = "stagingprosecutors.event.cps-update-cotr-received";
    private static final String CPS_SERVE_PET_RECEIVED_METHOD = "onCpsServePetReceived";
    private static final String CPS_SERVE_BCM_RECEIVED_EVENT = "stagingprosecutors.event.cps-serve-bcm-received";
    private static final String CPS_SERVE_BCM_RECEIVED_METHOD = "onCpsServeBcmReceived";
    private static final String NO = "N";
    private static final UUID submissionId = randomUUID();
    public static final String PROSECUTOR_DEFENDANT_ID = "PDEFID001";
    public static final String OFFENCE_CODE = "CJSCODE001";
    public static final String OFFENCE_WORDING = "Test Offence Wording";
    public static final String OFFENCE_DATE = "2021-09-27";
    public static final String PROSECUTION_AUTHORITY = "OUCODE";
    public static final String URN = "TVL1234";
    public static final String PHONE = "8778888345";
    public static final String EMAIL = "test@test.com";
    public static final String NAME = "name";
    public static final String FORENAME = "forename";
    public static final String SURNAME = "surname";

    @Mock
    private Sender sender;

    @InjectMocks
    private CpsServeMaterialReceivedProcessor cpsServeMaterialReceivedProcessor;

    @Captor
    private ArgumentCaptor<Envelope<CpsPetReceivedDetails>> publicCaptorCpsServePetReceived;

    @Captor
    private ArgumentCaptor<Envelope<CpsServeBcmReceived>> publicCaptorCpsServeBcmReceived;

    @Captor
    private ArgumentCaptor<Envelope<CpsCotrReceivedDetails>> publicCaptorCpsServeCotrReceived;

    @Captor
    private ArgumentCaptor<Envelope<CpsUpdateCotrReceivedDetails>> publicCaptorCpsUpdateCotrReceived;

    private static CpsServePetReceived createCpsServePetReceived() {
        final List<ProsecutionWillRelyOn> prosecutionWillRelyOn = new ArrayList<>();

        prosecutionWillRelyOn.add(ProsecutionWillRelyOn.CCTV_EVIDENCE);

        return cpsServePetReceived().withSubmissionId(submissionId)
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
                .withReviewingLawyer(createCpsCaseContact())
                .withProsecutionCaseProgressionOfficer(createCpsCaseContact())

                .build();
    }

    private static CpsServeCotrReceived createCpsServeCotrReceived() {
        final List<ProsecutionWillRelyOn> prosecutionWillRelyOn = new ArrayList<>();

        prosecutionWillRelyOn.add(ProsecutionWillRelyOn.CCTV_EVIDENCE);

        final List<DefendantSubject> defendantSubjects = new ArrayList<>();
        defendantSubjects.add(DefendantSubject.defendantSubject()
                .withProsecutorDefendantId("prosecutor defendant id")
                .withProsecutorPersonDefendantDetails(ProsecutorPersonDefendantDetails.prosecutorPersonDefendantDetails()
                        .withProsecutorDefendantId(randomUUID().toString())
                        .withDateOfBirth("2007-05-04")
                        .withForename("Forename")
                        .withForename2("Forename2")
                        .withForename3("Forename3")
                        .withSurname("Surname")
                        .withLocalAuthorityDetailsForYouthDefendants(LocalAuthorityDetailsForYouthDefendants.localAuthorityDetailsForYouthDefendants()
                                .withForename(FORENAME)
                                .withSurname(SURNAME)
                                .build())
                        .withParentGuardianForYouthDefendants(DefendantParentGuardian.defendantParentGuardian()
                                .withForename(FORENAME)
                                .withSurname(SURNAME)
                                .build())
                        .build())
                .withCpsPersonDefendantDetails(CpsPersonDefendantDetails.cpsPersonDefendantDetails()
                        .withDateOfBirth("2011-07-07")
                        .withForename("Forename")
                        .withForename2("Forename2")
                        .withForename3("Forename3")
                        .withSurname("Surname")
                        .withCpsDefendantId(randomUUID().toString())
                        .withLocalAuthorityDetailsForYouthDefendants(LocalAuthorityDetailsForYouthDefendants.localAuthorityDetailsForYouthDefendants()
                                .withForename(FORENAME)
                                .withSurname(SURNAME)
                                .build())
                        .withParentGuardianForYouthDefendants(DefendantParentGuardian.defendantParentGuardian()
                                .withForename(FORENAME)
                                .withSurname(SURNAME)
                                .build())
                        .build())
                .build());

        return CpsServeCotrReceived.cpsServeCotrReceived().withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
                .withDefendantSubject(defendantSubjects)
                .build();
    }

    private static CpsUpdateCotrReceived createCpsUpdateCotrReceived() {
        final List<DefendantSubject> defendantSubjects = new ArrayList<>();
        defendantSubjects.add(DefendantSubject.defendantSubject()
                .withProsecutorDefendantId("prosecutor defendant id")
                .withProsecutorPersonDefendantDetails(ProsecutorPersonDefendantDetails.prosecutorPersonDefendantDetails()
                        .withProsecutorDefendantId(randomUUID().toString())
                        .withDateOfBirth("2007-05-04")
                        .withForename("Forename")
                        .withForename2("Forename2")
                        .withForename3("Forename3")
                        .withSurname("Surname")
                        .withLocalAuthorityDetailsForYouthDefendants(LocalAuthorityDetailsForYouthDefendants.localAuthorityDetailsForYouthDefendants()
                                .withForename(FORENAME)
                                .withSurname(SURNAME)
                                .build())
                        .withParentGuardianForYouthDefendants(DefendantParentGuardian.defendantParentGuardian()
                                .withForename(FORENAME)
                                .withSurname(SURNAME)
                                .build())
                        .build())
                .withCpsPersonDefendantDetails(CpsPersonDefendantDetails.cpsPersonDefendantDetails()
                        .withDateOfBirth("2011-07-07")
                        .withForename("Forename")
                        .withForename2("Forename2")
                        .withForename3("Forename3")
                        .withSurname("Surname")
                        .withCpsDefendantId(randomUUID().toString())
                        .withLocalAuthorityDetailsForYouthDefendants(LocalAuthorityDetailsForYouthDefendants.localAuthorityDetailsForYouthDefendants()
                                .withForename(FORENAME)
                                .withSurname(SURNAME)
                                .build())
                        .withParentGuardianForYouthDefendants(DefendantParentGuardian.defendantParentGuardian()
                                .withForename(FORENAME)
                                .withSurname(SURNAME)
                                .build())
                        .build())
                .build());
        return CpsUpdateCotrReceived.cpsUpdateCotrReceived().withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
                .withDefendantSubject(defendantSubjects)
                .withCertifyThatTheProsecutionIsTrialReady("TrialReady")
                .build();
    }

    private static CpsServeBcmReceived createCpsServeBcmReceived() {
        return cpsServeBcmReceived().withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
                .withDefendantOffencesSubject(createDefendantOffencesSubjects())
                .withEvidencePrePTPH("Evidence")
                .build();
    }

    private static List<DefendantOffencesSubjects> createDefendantOffencesSubjects() {
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
                        .withCpsDefendantId("af4bc5bb-9393-462a-9673-e40ac2218552")
                        .build())
                .withOffences(offence)
                .build());

        return defendantOffencesSubjects;
    }

    private static CpsProsecutionCaseSubject createCpsProsecutionCaseSubject() {
        return CpsProsecutionCaseSubject.cpsProsecutionCaseSubject()
                .withUrn("TVL1234")
                .withProsecutingAuthority("OUCODE")
                .build();
    }

    @Test
    public void shouldHandleCpsServePetReceivedEvent() {
        assertThat(cpsServeMaterialReceivedProcessor, isHandler(EVENT_PROCESSOR)
                .with(method(CPS_SERVE_PET_RECEIVED_METHOD)
                        .thatHandles(CPS_SERVE_PET_RECEIVED_EVENT)
                ));
    }

    @Test
    public void shouldRaisePublicEventOnServePetReceived() {
        final CpsServePetReceived cpsServePetReceived = createCpsServePetReceived();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName(CPS_SERVE_PET_RECEIVED_EVENT)
                .withId(randomUUID())
                .build();

        final Envelope<CpsServePetReceived> cpsServePetReceivedEnvelope =
                envelopeFrom(metadata, cpsServePetReceived);
        cpsServeMaterialReceivedProcessor.onCpsServePetReceived(cpsServePetReceivedEnvelope);

        verify(sender).send(publicCaptorCpsServePetReceived.capture());
        final Envelope<CpsPetReceivedDetails> publicEventEnvelope = publicCaptorCpsServePetReceived.getValue();
        final CpsPetReceivedDetails cpsPetReceivedDetails = publicEventEnvelope.payload();
        verifyPetReceivedPublicEvent(cpsPetReceivedDetails);

    }

    @Test
    public void shouldHandleCpsServeBcmReceivedEvent() {
        assertThat(cpsServeMaterialReceivedProcessor, isHandler(EVENT_PROCESSOR)
                .with(method(CPS_SERVE_BCM_RECEIVED_METHOD)
                        .thatHandles(CPS_SERVE_BCM_RECEIVED_EVENT)
                ));
    }

    @Test
    public void shouldRaisePublicEventOnServeBcmReceived() {
        final CpsServeBcmReceived cpsServeBcmReceived = createCpsServeBcmReceived();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName(CPS_SERVE_BCM_RECEIVED_EVENT)
                .withId(randomUUID())
                .build();

        final Envelope<CpsServeBcmReceived> cpsServeBcmReceivedEnvelope =
                envelopeFrom(metadata, cpsServeBcmReceived);
        cpsServeMaterialReceivedProcessor.onCpsServeBcmReceived(cpsServeBcmReceivedEnvelope);

        verify(sender).send(publicCaptorCpsServeBcmReceived.capture());
    }

    @Test
    public void shouldRaisePublicEventOnServeCotrReceived() {
        final CpsServeCotrReceived cpsServeCotrReceived = createCpsServeCotrReceived();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName(CPS_SERVE_COTR_RECEIVED_EVENT)
                .withId(randomUUID())
                .build();
        final Envelope<CpsServeCotrReceived> cpsServeCotrReceivedEnvelope =
                envelopeFrom(metadata, cpsServeCotrReceived);
        cpsServeMaterialReceivedProcessor.onCpsServeCotrReceived(cpsServeCotrReceivedEnvelope);

        verify(sender).send(publicCaptorCpsServeCotrReceived.capture());
        final Envelope<CpsCotrReceivedDetails> publicEventEnvelope = publicCaptorCpsServeCotrReceived.getValue();
        final CpsCotrReceivedDetails cpsCotrReceivedDetails = publicEventEnvelope.payload();
        assertThat(cpsCotrReceivedDetails.getDefendantSubject().get(0).getMatchingId(), is(notNullValue()));
    }

    @Test
    public void shouldRaisePublicEventOnUpdateCotrReceived() {
        final CpsUpdateCotrReceived cpsUpdateCotrReceived = createCpsUpdateCotrReceived();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName(CPS_UPDATE_COTR_RECEIVED_EVENT)
                .withId(randomUUID())
                .build();
        final Envelope<CpsUpdateCotrReceived> cpsUpdateCotrReceivedEnvelope =
                envelopeFrom(metadata, cpsUpdateCotrReceived);
        cpsServeMaterialReceivedProcessor.onCpsUpdateCotrReceived(cpsUpdateCotrReceivedEnvelope);

        verify(sender).send(publicCaptorCpsUpdateCotrReceived.capture());
        final Envelope<CpsUpdateCotrReceivedDetails> publicEventEnvelope = publicCaptorCpsUpdateCotrReceived.getValue();
        final CpsUpdateCotrReceivedDetails cpsUpdateCotrReceivedDetails = publicEventEnvelope.payload();
        assertThat(cpsUpdateCotrReceivedDetails.getDefendantSubject().get(0).getMatchingId(), is(notNullValue()));
    }

    private void verifyPetReceivedPublicEvent(final CpsPetReceivedDetails cpsPetReceivedDetails) {
        assertThat(cpsPetReceivedDetails, notNullValue());
        assertThat(cpsPetReceivedDetails.getCpsDefendantOffences(), notNullValue());
        final List<CpsDefendantOffences> cpsDefendantOffencesList = cpsPetReceivedDetails.getCpsDefendantOffences();
        assertThat(cpsDefendantOffencesList, hasSize(1));
        cpsDefendantOffencesList.forEach(this::verifyCpsDefendantOffenceObject);

        assertThat(cpsPetReceivedDetails.getProsecutionCaseSubject(), notNullValue());
        assertThat(cpsPetReceivedDetails.getProsecutionCaseSubject().getProsecutingAuthority(), is(PROSECUTION_AUTHORITY));
        assertThat(cpsPetReceivedDetails.getProsecutionCaseSubject().getUrn(), is(URN));
        assertThat(cpsPetReceivedDetails.getSubmissionId(), is(submissionId));
        assertThat(cpsPetReceivedDetails.getSubmissionStatus().toString(), is(PENDING.toString()));
        assertThat(cpsPetReceivedDetails.getPetFormData(), notNullValue());

        assertThat(cpsPetReceivedDetails.getPetFormData().getDefence(), is(notNullValue()));
        assertThat(cpsPetReceivedDetails.getReviewingLawyer(), notNullValue());
        assertThat(cpsPetReceivedDetails.getReviewingLawyer().getName(), is(NAME));
        assertThat(cpsPetReceivedDetails.getReviewingLawyer().getEmail(), is(EMAIL));
        assertThat(cpsPetReceivedDetails.getReviewingLawyer().getPhone(), is(PHONE));
        assertThat(cpsPetReceivedDetails.getProsecutionCaseProgressionOfficer(), notNullValue());
        assertThat(cpsPetReceivedDetails.getProsecutionCaseProgressionOfficer().getName(), is(NAME));
        assertThat(cpsPetReceivedDetails.getProsecutionCaseProgressionOfficer().getEmail(), is(EMAIL));
        assertThat(cpsPetReceivedDetails.getProsecutionCaseProgressionOfficer().getPhone(), is(PHONE));
        verifyPetFormDataInPublicEvent(cpsPetReceivedDetails.getPetFormData());
    }

    private void verifyPetFormDataInPublicEvent(final PetFormData petFormData) {
        final Defence defence = petFormData.getDefence();
        assertThat(defence.getDefendants(), hasSize(1));
        assertThat(defence.getDefendants().get(0).getCpsDefendantId(), is(CPS_DEFENDANT_ID));
        assertThat(defence.getDefendants().get(0).getProsecutorDefendantId(), is(PROSECUTOR_DEFENDANT_ID));
        assertThat(defence.getDefendants().get(0).getId(), is(nullValue()));
        assertThat(defence.getDefendants().get(0).getCpsOffences(), hasSize(1));
        assertThat(defence.getDefendants().get(0).getCpsOffences().get(0).getOffenceCode(), is(OFFENCE_CODE));
        assertThat(defence.getDefendants().get(0).getCpsOffences().get(0).getWording(), is(OFFENCE_WORDING));
        assertThat(defence.getDefendants().get(0).getCpsOffences().get(0).getDate(), is(notNullValue()));

        assertThat(petFormData.getProsecution(), is(notNullValue()));
        final Prosecution prosecution = petFormData.getProsecution();

        assertThat(prosecution.getDynamicFormAnswers(), is(notNullValue()));
        assertThat(prosecution.getDynamicFormAnswers().getApplicationsForDirectionsGroup(), is(notNullValue()));
        final ApplicationsForDirectionsGroup applicationsForDirectionsGroup = prosecution.getDynamicFormAnswers().getApplicationsForDirectionsGroup();
        assertThat(applicationsForDirectionsGroup.getVariationStandardDirectionsProsecutor(), is(NO));
        assertThat(applicationsForDirectionsGroup.getGroundRulesQuestioning(), is(NO));

        assertThat(prosecution.getDynamicFormAnswers().getProsecutorGroup(), is(notNullValue()));
        final ProsecutorGroup prosecutorGroup = prosecution.getDynamicFormAnswers().getProsecutorGroup();
        assertThat(prosecutorGroup.getProsecutorServeEvidence(), is(NO));
        assertThat(prosecutorGroup.getProsecutionCompliance(), is(NO));
        assertThat(prosecutorGroup.getSlaveryOrExploitation(), is(NO));
    }

    private void verifyCpsDefendantOffenceObject(final CpsDefendantOffences cpsDefendantOffence) {
        assertThat(cpsDefendantOffence.getCpsDefendantId(), is(CPS_DEFENDANT_ID));
        assertThat(cpsDefendantOffence.getProsecutorDefendantId(), is(PROSECUTOR_DEFENDANT_ID));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails(), hasSize(1));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getCjsOffenceCode(), is(OFFENCE_CODE));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceDate(), is(DateUtil.convertToLocalDate(OFFENCE_DATE)));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceWording(), is(OFFENCE_WORDING));
    }

    private static CpsCaseContact createCpsCaseContact() {
        return CpsCaseContact.cpsCaseContact()
                .withEmail(EMAIL)
                .withPhone(PHONE)
                .withName(NAME)
                .build();
    }
}
