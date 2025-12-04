package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePtphReceived.cpsServePtphReceived;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsProsecutionCaseSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsProsecutionCaseSubjectPtph;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePtphReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantOffencesSubjectsPtph;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceSubject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CpsServePtphReceivedProcessorTest {

    private static final String CPS_SERVE_PTPH_RECEIVED_EVENT = "stagingprosecutors.event.cps-serve-ptph-received";
    private static final String CPS_SERVE_PTPH_RECEIVED_METHOD = "onCpsServePtphReceived";
    private static final UUID submissionId = randomUUID();

    @Mock
    private Sender sender;

    @InjectMocks
    private CpsServeMaterialReceivedProcessor cpsServePtphReceivedProcessor;

    @Captor
    private ArgumentCaptor<Envelope<CpsServePtphReceived>> publicCaptor;

    private static final String caseUrn = "TVL1234";

    @Test
    public void shouldHandleCCProsecutionReceivedEvent() {
        assertThat(cpsServePtphReceivedProcessor, isHandler(EVENT_PROCESSOR)
                .with(method(CPS_SERVE_PTPH_RECEIVED_METHOD)
                        .thatHandles(CPS_SERVE_PTPH_RECEIVED_EVENT)
                ));
    }

    @Test
    public void shouldRaisePublicEventOnServePtphReceived() {
        final CpsServePtphReceived cpsServePtphReceived = createCpsServePtphReceived();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName(CPS_SERVE_PTPH_RECEIVED_EVENT)
                .withId(randomUUID())
                .build();

        final Envelope<CpsServePtphReceived> cpsServePtphReceivedEnvelope =
                envelopeFrom(metadata, cpsServePtphReceived);
        cpsServePtphReceivedProcessor.onCpsServePtphReceived(cpsServePtphReceivedEnvelope);

        verify(sender).send(publicCaptor.capture());
    }

    private CpsServePtphReceived createCpsServePtphReceived() {
        return cpsServePtphReceived().withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .withProsecutionCaseSubject(createCpsProsecutionCaseSubjectPtph())
                .withDefendantOffencesSubjects(createDefendantOffencesSubjects())
                .build();
    }

    private List<DefendantOffencesSubjectsPtph> createDefendantOffencesSubjects() {
        List<DefendantOffencesSubjectsPtph> defendantOffencesSubjects = new ArrayList<>();
        final List<OffenceSubject> offence = new ArrayList<>();

        offence.add(OffenceSubject.offenceSubject()
                .withCjsOffenceCode("CJSCODE001")
                .withOffenceDate("2021-09-27")
                .withOffenceWording("Test Offence Wording")
                .build());

        defendantOffencesSubjects.add(DefendantOffencesSubjectsPtph.defendantOffencesSubjectsPtph()
                .withDefendant(DefendantSubject.defendantSubject()
                        .withProsecutorDefendantId("PDEFID001")
                        .build())
                .withPrincipalCharges("test")
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
}
