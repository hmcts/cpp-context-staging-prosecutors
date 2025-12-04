package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem.problem;
import static uk.gov.moj.cps.stagingprosecutors.domain.event.PublicMaterialPendingWithWarnings.publicMaterialPendingWithWarnings;
import static uk.gov.moj.cps.stagingprosecutors.domain.event.PublicMaterialRejected.publicMaterialRejected;
import static uk.gov.moj.cps.stagingprosecutors.domain.event.PublicMaterialRejectedV2.publicMaterialRejectedV2;
import static uk.gov.moj.cps.stagingprosecutors.domain.event.PublicMaterialRejectedWithWarnings.publicMaterialRejectedWithWarnings;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.RejectMaterial;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.UpdateSubmissionStatus;
import uk.gov.moj.cps.stagingprosecutors.domain.event.CpsServeMaterialStatusUpdatedEvent;
import uk.gov.moj.cps.stagingprosecutors.domain.event.PublicMaterialPendingWithWarnings;
import uk.gov.moj.cps.stagingprosecutors.domain.event.PublicMaterialRejected;
import uk.gov.moj.cps.stagingprosecutors.domain.event.PublicMaterialRejectedV2;
import uk.gov.moj.cps.stagingprosecutors.domain.event.PublicMaterialRejectedWithWarnings;
import uk.gov.moj.cps.stagingprosecutors.domain.event.SubmissionStatus;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseFilePublicEventProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<RejectMaterial>> jsonEnvelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope<UpdateSubmissionStatus>> updateSubmissionStatusArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope<PublicMaterialPendingWithWarnings>> pendingArgumentCaptor;

    @InjectMocks
    private ProsecutionCaseFilePublicEventProcessor prosecutionCaseFilePublicEventProcessor;

    private final UUID submissionId = randomUUID();

    @Test
    public void shouldHandlePublicSjpCaseCreatedEvent() {
        assertThat(prosecutionCaseFilePublicEventProcessor, isHandler(EVENT_PROCESSOR)
                .with(method("caseMaterialRejected").thatHandles("public.prosecutioncasefile.material-rejected")));
    }

    @Test
    public void shouldSendMaterialRejectedCommandWhenSubmissionIdIsPresent() {
        final Envelope<PublicMaterialRejected> publicMaterialRejectedEvent = givenPublicMaterialRejectedWithSubmissionId();
        whenPublicEventEmitted(publicMaterialRejectedEvent);
        thenRejectMaterialCommandIsSent(publicMaterialRejectedEvent);
    }

    @Test
    public void shouldIgnorePublicMaterialRejectedEventIfSubmissionIdIsNotPresent() {
        final Envelope<PublicMaterialRejected> publicMaterialRejectedEvent = givenPublicMaterialRejectedWithoutSubmissionId();
        whenPublicEventEmitted(publicMaterialRejectedEvent);
        thenRejectMaterialCommandIsNotSent();
    }

    @Test
    public void shouldHandlePublicSubmissionStatusUpdatedEvent() {
        assertThat(prosecutionCaseFilePublicEventProcessor, isHandler(EVENT_PROCESSOR)
                .with(method("cpsServeMaterialUpdateStatus").thatHandles("public.prosecutioncasefile.cps-serve-material-status-updated")));
    }

    @Test
    public void shouldSendSubmissionStatusUpdatedSuccessCommand() {
        final Envelope<CpsServeMaterialStatusUpdatedEvent> cpsServeMaterialStatusUpdatedEnvelope = givenCpsServeMaterialStatusUpdatedWithSubmissionId(SubmissionStatus.SUCCESS);
        prosecutionCaseFilePublicEventProcessor.cpsServeMaterialUpdateStatus(cpsServeMaterialStatusUpdatedEnvelope);
        verify(sender).send(updateSubmissionStatusArgumentCaptor.capture());

        final Envelope<UpdateSubmissionStatus> updateSubmissionStatusEnvelope = updateSubmissionStatusArgumentCaptor.getValue();

        assertThat(updateSubmissionStatusEnvelope.metadata().name(), is("stagingprosecutors.command.update-submission-status"));
        final UpdateSubmissionStatus updateSubmissionStatusPayload = updateSubmissionStatusEnvelope.payload();
        assertThat(updateSubmissionStatusPayload.getSubmissionId(), is(submissionId));
        assertThat(updateSubmissionStatusPayload.getSubmissionStatus().toString(), is(cpsServeMaterialStatusUpdatedEnvelope.payload().getSubmissionStatus().toString()));
        assertThat(updateSubmissionStatusPayload.getErrors(), is(nullValue()));
    }

    @Test
    public void shouldSendSubmissionStatusUpdatedFailedCommandOnExpiry() {
        final Envelope<CpsServeMaterialStatusUpdatedEvent> cpsServeMaterialStatusUpdatedEnvelope = givenCpsServeMaterialStatusUpdatedWithSubmissionId(SubmissionStatus.EXPIRED);
        prosecutionCaseFilePublicEventProcessor.cpsServeMaterialUpdateStatus(cpsServeMaterialStatusUpdatedEnvelope);
        verify(sender).send(updateSubmissionStatusArgumentCaptor.capture());

        final Envelope<UpdateSubmissionStatus> updateSubmissionStatusEnvelope = updateSubmissionStatusArgumentCaptor.getValue();

        assertThat(updateSubmissionStatusEnvelope.metadata().name(), is("stagingprosecutors.command.update-submission-status"));
        final UpdateSubmissionStatus updateSubmissionStatusPayload = updateSubmissionStatusEnvelope.payload();
        assertThat(updateSubmissionStatusPayload.getSubmissionId(), is(submissionId));
        assertThat(updateSubmissionStatusPayload.getSubmissionStatus().toString(), is("FAILED"));
        assertThat(updateSubmissionStatusPayload.getErrors(), is(notNullValue()));
    }

    @Test
    public void shouldHandlePublicSjpCaseCreatedEvent1() {
        assertThat(prosecutionCaseFilePublicEventProcessor, isHandler(EVENT_PROCESSOR)
                .with(method("caseMaterialRejectedV2").thatHandles("public.prosecutioncasefile.material-rejected-v2")));
    }

    @Test
    public void shouldSendMaterialRejectedCommandWhenSubmissionIdIsPresentV2() {
        final Envelope<PublicMaterialRejectedV2> event = givenPublicMaterialRejectedV2WithSubmissionId();
        prosecutionCaseFilePublicEventProcessor.caseMaterialRejectedV2(event);
        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());

        final Envelope<RejectMaterial> rejectMaterialCommand = jsonEnvelopeArgumentCaptor.getValue();

        assertThat(rejectMaterialCommand.metadata().name(), is("stagingprosecutors.command.reject-material"));

        final RejectMaterial computedPayload = rejectMaterialCommand.payload();
        assertThat(computedPayload.getSubmissionId(), is(submissionId));
        assertThat(computedPayload.getErrors(), is(event.payload().getErrors()));
    }

    @Test
    public void shouldIgnorePublicMaterialRejectedV2EventIfSubmissionIdIsNotPresent() {
        final Metadata metadataWithoutSubmissionId = metadataWithRandomUUID("public.prosecutioncasefile.material-rejected-v2").build();
        final PublicMaterialRejectedV2 materialRejectedV2 = publicMaterialRejectedV2().withErrors(asList(problem().build())).build();
        final Envelope<PublicMaterialRejectedV2> event = envelopeFrom(metadataWithoutSubmissionId, materialRejectedV2);

        prosecutionCaseFilePublicEventProcessor.caseMaterialRejectedV2(event);
        verify(sender, never()).send(argThat(any(Envelope.class)));
    }

    @Test
    public void shouldSendMaterialRejectedCommandWhenSubmissionIdIsPresentWithWarnings() {
        final Envelope<PublicMaterialRejectedWithWarnings> event = givenPublicMaterialRejectedWithWarningsAndWithSubmissionId();
        prosecutionCaseFilePublicEventProcessor.caseMaterialRejectedWithWarnings(event);
        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());

        final Envelope<RejectMaterial> rejectMaterialCommand = jsonEnvelopeArgumentCaptor.getValue();

        assertThat(rejectMaterialCommand.metadata().name(), is("stagingprosecutors.command.reject-material"));

        final RejectMaterial computedPayload = rejectMaterialCommand.payload();
        assertThat(computedPayload.getSubmissionId(), is(submissionId));
        assertThat(computedPayload.getErrors(), is(event.payload().getErrors()));
        assertThat(computedPayload.getWarnings(), is(event.payload().getWarnings()));
    }

    @Test
    public void shouldIgnorePublicMaterialRejectedWithWarningsEventIfSubmissionIdIsNotPresent() {
        final Metadata metadataWithoutSubmissionId = metadataWithRandomUUID("public.prosecutioncasefile.material-rejected-with-warnings").build();
        final PublicMaterialRejectedWithWarnings publicMaterialRejectedWithWarnings = publicMaterialRejectedWithWarnings()
                .withErrors(asList(problem().build()))
                .withWarnings(asList(problem().build()))
                .build();
        final Envelope<PublicMaterialRejectedWithWarnings> event = envelopeFrom(metadataWithoutSubmissionId, publicMaterialRejectedWithWarnings);

        prosecutionCaseFilePublicEventProcessor.caseMaterialRejectedWithWarnings(event);
        verify(sender, never()).send(argThat(any(Envelope.class)));
    }

    @Test
    public void shouldHandlePublicMaterialPendingWithWarnings() {
        final Envelope<PublicMaterialPendingWithWarnings> event = givenPublicMaterialPendingWithWarningsAndWithSubmissionId();
        prosecutionCaseFilePublicEventProcessor.materialSubmissionPendingWithWarnings(event);
        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());

        final Envelope<RejectMaterial> rejectMaterialCommand = jsonEnvelopeArgumentCaptor.getValue();

        assertThat(rejectMaterialCommand.metadata().name(), is("stagingprosecutors.command.material-pending-with-warnings"));

        final RejectMaterial computedPayload = rejectMaterialCommand.payload();
        assertThat(computedPayload.getSubmissionId(), is(submissionId));
        assertThat(computedPayload.getWarnings(), is(event.payload().getWarnings()));
    }

    @Test
    public void shouldIgnorePublicMaterialPendingWithWarningsIfSubmissionIdIsNotPresent() {
        final Metadata metadataWithoutSubmissionId = metadataWithRandomUUID("public.prosecutioncasefile.material-pending-with-warnings").build();
        final PublicMaterialPendingWithWarnings publicMaterialPendingWithWarnings = publicMaterialPendingWithWarnings()
                .withWarnings(asList(problem().build()))
                .build();
        final Envelope<PublicMaterialPendingWithWarnings> event = envelopeFrom(metadataWithoutSubmissionId, publicMaterialPendingWithWarnings);

        prosecutionCaseFilePublicEventProcessor.materialSubmissionPendingWithWarnings(event);
        verify(sender, never()).send(argThat(any(Envelope.class)));
    }

    private Envelope<PublicMaterialRejected> givenPublicMaterialRejectedWithSubmissionId() {
        final Metadata basicMetadata = metadataWithRandomUUID("public.prosecutioncasefile.material-rejected").build();
        final Metadata metadataWithSubmissionId = metadataFrom(createObjectBuilder(basicMetadata.asJsonObject()).add("submissionId", submissionId.toString()).build())
                .build();
        return getPublicMaterialRejectedEvent(metadataWithSubmissionId);
    }

    private Envelope<PublicMaterialRejectedV2> givenPublicMaterialRejectedV2WithSubmissionId() {
        final Metadata basicMetadata = metadataWithRandomUUID("public.prosecutioncasefile.material-rejected-v2").build();
        final Metadata metadataWithSubmissionId = metadataFrom(createObjectBuilder(basicMetadata.asJsonObject()).add("submissionId", submissionId.toString()).build())
                .build();
        return getPublicMaterialRejectedEventV2(metadataWithSubmissionId);
    }

    private Envelope<PublicMaterialRejectedWithWarnings> givenPublicMaterialRejectedWithWarningsAndWithSubmissionId() {
        final Metadata basicMetadata = metadataWithRandomUUID("public.prosecutioncasefile.material-rejected-with-warnings").build();
        final Metadata metadataWithSubmissionId = metadataFrom(createObjectBuilder(basicMetadata.asJsonObject()).add("submissionId", submissionId.toString()).build())
                .build();
        return getPublicMaterialRejectedWithWarnings(metadataWithSubmissionId);
    }

    private Envelope<PublicMaterialPendingWithWarnings> givenPublicMaterialPendingWithWarningsAndWithSubmissionId() {
        final Metadata basicMetadata = metadataWithRandomUUID("public.prosecutioncasefile.material-pending-with-warnings").build();
        final Metadata metadataWithSubmissionId = metadataFrom(createObjectBuilder(basicMetadata.asJsonObject()).add("submissionId", submissionId.toString()).build())
                .build();
        return getPublicMaterialPendingWithWarnings(metadataWithSubmissionId);
    }


    private Envelope<PublicMaterialRejected> givenPublicMaterialRejectedWithoutSubmissionId() {
        final Metadata metadataWithoutSubmissionId = metadataWithRandomUUID("public.prosecutioncasefile.material-rejected").build();
        return getPublicMaterialRejectedEvent(metadataWithoutSubmissionId);
    }

    private void whenPublicEventEmitted(final Envelope<PublicMaterialRejected> eveent) {
        prosecutionCaseFilePublicEventProcessor.caseMaterialRejected(eveent);
    }

    private void thenRejectMaterialCommandIsNotSent() {
        verify(sender, never()).send(argThat(any(Envelope.class)));
    }

    private void thenRejectMaterialCommandIsSent(final Envelope<PublicMaterialRejected> eveent) {
        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());

        final Envelope<RejectMaterial> rejectMaterialCommand = jsonEnvelopeArgumentCaptor.getValue();

        assertThat(rejectMaterialCommand.metadata().name(), is("stagingprosecutors.command.reject-material"));
        final RejectMaterial computedPayload = rejectMaterialCommand.payload();
        assertThat(computedPayload.getSubmissionId(), is(submissionId));
        assertThat(computedPayload.getErrors(), is(eveent.payload().getErrors()));
    }

    private static Envelope<PublicMaterialRejectedV2> getPublicMaterialRejectedEventV2(final Metadata metadata) {
        final PublicMaterialRejectedV2 materialRejectedV2 = publicMaterialRejectedV2().withErrors(asList(problem().build())).build();
        return envelopeFrom(metadata, materialRejectedV2);
    }

    private static Envelope<PublicMaterialRejectedWithWarnings> getPublicMaterialRejectedWithWarnings(final Metadata metadata) {
        final PublicMaterialRejectedWithWarnings publicMaterialRejectedWithWarnings = publicMaterialRejectedWithWarnings()
                .withErrors(asList(problem().build()))
                .withWarnings(asList(problem().build()))
                .build();
        return envelopeFrom(metadata, publicMaterialRejectedWithWarnings);
    }

    private static Envelope<PublicMaterialPendingWithWarnings> getPublicMaterialPendingWithWarnings(final Metadata metadata) {
        final PublicMaterialPendingWithWarnings publicMaterialPendingWithWarnings = publicMaterialPendingWithWarnings()
                .withWarnings(asList(problem().build()))
                .build();
        return envelopeFrom(metadata, publicMaterialPendingWithWarnings);
    }

    private static Envelope<PublicMaterialRejected> getPublicMaterialRejectedEvent(final Metadata metadata) {
        final PublicMaterialRejected providedMaterialRejectedEvent = publicMaterialRejected().withErrors(asList(problem().build())).build();
        return envelopeFrom(metadata, providedMaterialRejectedEvent);
    }

    private Envelope<CpsServeMaterialStatusUpdatedEvent> givenCpsServeMaterialStatusUpdatedWithSubmissionId(final SubmissionStatus submissionStatus) {
        final Metadata basicMetadata = metadataWithRandomUUID("public.prosecutioncasefile.cps-serve-material-status-updated").build();
        final Metadata metadataWithSubmissionId = metadataFrom(createObjectBuilder(basicMetadata.asJsonObject()).add("submissionId", submissionId.toString()).build())
                .build();
        final CpsServeMaterialStatusUpdatedEvent cpsServeMaterialStatusUpdated = CpsServeMaterialStatusUpdatedEvent.cpsServeMaterialStatusUpdatedEvent()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(submissionStatus)
                .build();
        return envelopeFrom(metadataWithSubmissionId, cpsServeMaterialStatusUpdated);
    }
}
