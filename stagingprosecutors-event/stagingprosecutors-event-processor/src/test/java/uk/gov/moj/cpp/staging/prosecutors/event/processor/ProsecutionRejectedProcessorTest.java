package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.ProsecutionRejectedProcessor.REJECT_SUBMISSION_COMMAND;
import static uk.gov.moj.cps.stagingprosecutors.domain.event.PublicProsecutionRejected.publicProsecutionRejected;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.RejectSubmission;
import uk.gov.moj.cps.stagingprosecutors.domain.event.PublicProsecutionRejected;

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
public class ProsecutionRejectedProcessorTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private ProsecutionRejectedProcessor prosecutionRejectedProcessor;

    @Captor
    private ArgumentCaptor<Envelope<RejectSubmission>> captor;

    @Test
    public void onProsecutionRejected() {
        final PublicProsecutionRejected prosecutionRejected = buildPublicProsecution().build();

        final Envelope<PublicProsecutionRejected> publicProsecutionRejectedEnvelope = envelopeFrom(buildMetadata(), prosecutionRejected);

        prosecutionRejectedProcessor.onProsecutionRejected(publicProsecutionRejectedEnvelope);

        verify(sender).send(captor.capture());

        final Envelope<RejectSubmission> rejectSubmissionEnvelop = captor.getValue();

        final Metadata envelopMetadata = rejectSubmissionEnvelop.metadata();
        assertThat(envelopMetadata.name(), is(REJECT_SUBMISSION_COMMAND));

        final RejectSubmission envelopPayload = rejectSubmissionEnvelop.payload();
        assertThat(envelopPayload.getSubmissionId(), is(prosecutionRejected.getExternalId()));
        assertThat(envelopPayload.getErrors(), containsInAnyOrder(getProblem()));
    }

    @Test
    public void onNonProsecutionRejected() {

        final DefendantProblem defendantProblem = DefendantProblem.defendantProblem()
                .withProblems(ImmutableList.of(getProblem()))
                .withProsecutorDefendantReference(randomUUID().toString())
                .build();
        final PublicProsecutionRejected prosecutionRejected = buildPublicProsecution()
                .withCaseErrors(ImmutableList.of(getProblem()))
                .withDefendantErrors(ImmutableList.of(defendantProblem))
                .build();

        final Envelope<PublicProsecutionRejected> publicProsecutionRejectedEnvelope = envelopeFrom(buildMetadata(), prosecutionRejected);

        prosecutionRejectedProcessor.onProsecutionRejected(publicProsecutionRejectedEnvelope);

        verify(sender).send(captor.capture());

        final Envelope<RejectSubmission> rejectSubmissionEnvelop = captor.getValue();

        final Metadata envelopMetadata = rejectSubmissionEnvelop.metadata();
        assertThat(envelopMetadata.name(), is(REJECT_SUBMISSION_COMMAND));

        final RejectSubmission envelopPayload = rejectSubmissionEnvelop.payload();
        assertThat(envelopPayload.getSubmissionId(), is(prosecutionRejected.getExternalId()));
        assertThat(envelopPayload.getCaseErrors(), containsInAnyOrder(getProblem()));
        assertThat(envelopPayload.getDefendantErrors(), containsInAnyOrder(defendantProblem));
    }

    @Test
    public void onProsecutionRejectedWhenExternalIdIsNull() {

        final PublicProsecutionRejected publicProsecutionRejected = buildPublicProsecution().withExternalId(null).build();
        final Envelope<PublicProsecutionRejected> publicProsecutionRejectedEnvelope = envelopeFrom(buildMetadata(), publicProsecutionRejected);

        prosecutionRejectedProcessor.onProsecutionRejected(publicProsecutionRejectedEnvelope);
        verifyNoInteractions(sender);
    }

    @Test
    public void onProsecutionRejectedWhenChannelIsNotCPPI() {

        final PublicProsecutionRejected publicProsecutionRejected = buildPublicProsecution().withChannel(SPI).build();
        final Envelope<PublicProsecutionRejected> publicProsecutionRejectedEnvelope = envelopeFrom(buildMetadata(), publicProsecutionRejected);

        prosecutionRejectedProcessor.onProsecutionRejected(publicProsecutionRejectedEnvelope);
        verifyNoInteractions(sender);
    }

    private PublicProsecutionRejected.Builder buildPublicProsecution() {
        return publicProsecutionRejected()
                .withCaseId(randomUUID())
                .withErrors(ImmutableList.of(getProblem()))
                .withExternalId(randomUUID())
                .withChannel(CPPI);
    }

    private Metadata buildMetadata() {
        final JsonObject initialMetadata = metadataBuilder()
                .withId(randomUUID())
                .withName("public.prosecutioncasefile.prosecution-rejected")
                .build()
                .asJsonObject();

        return metadataFrom(JsonObjects.createObjectBuilder(initialMetadata).build()).build();
    }

    private Problem getProblem() {
        return Problem.problem()
                .withCode("problemCode")
                .build();
    }
}