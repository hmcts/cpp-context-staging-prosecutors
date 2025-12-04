package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_UTC_DATE_TIME;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsProsecutionReceived;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsSjpProsecutionReceived;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionReceived;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionReceivedProcessorTest {

    private static final String PROSECUTIONCASEFILE_COMMAND_INITIATE_CC_PROSECUTION = "prosecutioncasefile.command.initiate-cc-prosecution";
    private static final String PROSECUTIONCASEFILE_COMMAND_INITIATE_SJP_PROSECUTION = "prosecutioncasefile.command.initiate-sjp-prosecution";
    private static final String CC_PROSECUTION_RECEIVED_EVENT = "stagingprosecutors.event.prosecution-received";
    private static final String SJP_PROSECUTION_RECEIVED_EVENT = "stagingprosecutors.event.sjp-prosecution-received";
    private static final String CC_PROSECUTION_RECEIVED_METHOD = "onProsecutionReceived";
    private static final String SJP_PROSECUTION_RECEIVED_METHOD = "onSjpProsecutionReceived";
    private static final UUID CASE_FILE_ID = randomUUID();

    @Mock
    private EnvelopeHelper envelopeHelper;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private Sender sender;

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @Spy
    private final StoppedClock clock = new StoppedClock(now());

    @InjectMocks
    private ProsecutionReceivedProcessor target;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<InitiateProsecution> payloadArgumentCaptor;

    @Test
    public void shouldHandleCCProsecutionReceivedEvent() {
        assertThat(target, isHandler(EVENT_PROCESSOR)
                .with(method(CC_PROSECUTION_RECEIVED_METHOD)
                        .thatHandles(CC_PROSECUTION_RECEIVED_EVENT)
                ));
    }

    @Test
    public void shouldHandleSjpProsecutionReceivedEvent() {
        assertThat(target, isHandler(EVENT_PROCESSOR)
                .with(method(SJP_PROSECUTION_RECEIVED_METHOD)
                        .thatHandles(SJP_PROSECUTION_RECEIVED_EVENT)
                ));
    }

    @Test
    public void shouldInitiateCCProsecutionCommandToPCF() {
        when(systemIdMapperService.getCppCaseIdFor(anyString())).thenReturn(CASE_FILE_ID);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        when(envelopeHelper.withMetadataInPayload(any())).thenReturn(jsonEnvelope);

        final ProsecutionReceived prosecutionReceived = prosecutorsProsecutionReceived();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<ProsecutionReceived> prosecutionReceivedEnvelope = testEnvelope(prosecutionReceived, CC_PROSECUTION_RECEIVED_EVENT,
                prosecutionReceived.getSubmissionId().toString(), eventCreatedTime);

        target.onProsecutionReceived(prosecutionReceivedEnvelope);

        verify(sender).sendAsAdmin(jsonEnvelope);
        verify(envelopeHelper).withMetadataInPayload(jsonEnvelopeArgumentCaptor.capture());

        final JsonEnvelope initiateCcProsecutionEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        final Metadata metadata = initiateCcProsecutionEnvelope.metadata();
        final JsonObject payload = initiateCcProsecutionEnvelope.payloadAsJsonObject();

        assertThat(metadata.name(), is(PROSECUTIONCASEFILE_COMMAND_INITIATE_CC_PROSECUTION));
        assertThat(metadata.asJsonObject().containsKey("submissionId"), is(false));
        assertThat(payload, is(jsonObject));

        verify(objectToJsonObjectConverter).convert(payloadArgumentCaptor.capture());
        final InitiateProsecution actualPayload = payloadArgumentCaptor.getValue();
        assertThat(actualPayload.getCaseDetails().getDateReceived(), is(eventCreatedTime.toLocalDate()));
        assertThat(actualPayload.getExternalId(), is(prosecutionReceived.getSubmissionId()));
    }

    @Test
    public void shouldInitiateSJPProsecutionCommandToPCF() {
        when(systemIdMapperService.getCppCaseIdFor(anyString())).thenReturn(CASE_FILE_ID);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        when(envelopeHelper.withMetadataInPayload(any())).thenReturn(jsonEnvelope);

        final SjpProsecutionReceived prosecutionReceived = prosecutorsSjpProsecutionReceived();
        final ZonedDateTime eventCreatedTime = PAST_UTC_DATE_TIME.next();
        final Envelope<SjpProsecutionReceived> prosecutionReceivedEnvelope = testEnvelope(prosecutionReceived, SJP_PROSECUTION_RECEIVED_EVENT,
                prosecutionReceived.getSubmissionId().toString(), eventCreatedTime);

        target.onSjpProsecutionReceived(prosecutionReceivedEnvelope);

        verify(sender).sendAsAdmin(jsonEnvelope);
        verify(envelopeHelper).withMetadataInPayload(jsonEnvelopeArgumentCaptor.capture());

        final JsonEnvelope initiateSjpProsecutionEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        final Metadata metadata = initiateSjpProsecutionEnvelope.metadata();
        final JsonObject payload = initiateSjpProsecutionEnvelope.payloadAsJsonObject();

        assertThat(metadata.name(), is(PROSECUTIONCASEFILE_COMMAND_INITIATE_SJP_PROSECUTION));
        assertThat(metadata.asJsonObject().containsKey("submissionId"), is(false));
        assertThat(payload, is(jsonObject));

        verify(objectToJsonObjectConverter).convert(payloadArgumentCaptor.capture());
        final InitiateProsecution actualPayload = payloadArgumentCaptor.getValue();
        assertThat(actualPayload.getCaseDetails().getDateReceived(), is(eventCreatedTime.toLocalDate()));
        assertThat(actualPayload.getExternalId(), is(prosecutionReceived.getSubmissionId()));
    }

    @Test
    public void shouldDefaultToCurrentDateWhenDateNotAvailableFromMetadataForCCProsecution() {
        when(systemIdMapperService.getCppCaseIdFor(anyString())).thenReturn(CASE_FILE_ID);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        when(envelopeHelper.withMetadataInPayload(any())).thenReturn(jsonEnvelope);
        final ProsecutionReceived prosecutionReceived = prosecutorsProsecutionReceived();
        final Envelope<ProsecutionReceived> prosecutionReceivedEnvelope = testEnvelope(prosecutionReceived, CC_PROSECUTION_RECEIVED_EVENT, prosecutionReceived.getSubmissionId().toString());

        target.onProsecutionReceived(prosecutionReceivedEnvelope);

        verify(objectToJsonObjectConverter).convert(payloadArgumentCaptor.capture());
        final InitiateProsecution actualPayload = payloadArgumentCaptor.getValue();
        assertThat(actualPayload.getCaseDetails().getDateReceived(), is(clock.now().toLocalDate()));
    }

    @Test
    public void shouldDefaultToCurrentDateWhenDateNotAvailableFromMetadataForSJPProsecution() {
        when(systemIdMapperService.getCppCaseIdFor(anyString())).thenReturn(CASE_FILE_ID);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        when(envelopeHelper.withMetadataInPayload(any())).thenReturn(jsonEnvelope);
        final SjpProsecutionReceived prosecutionReceived = prosecutorsSjpProsecutionReceived();
        final Envelope<SjpProsecutionReceived> prosecutionReceivedEnvelope = testEnvelope(prosecutionReceived, SJP_PROSECUTION_RECEIVED_EVENT, prosecutionReceived.getSubmissionId().toString());

        target.onSjpProsecutionReceived(prosecutionReceivedEnvelope);

        verify(objectToJsonObjectConverter).convert(payloadArgumentCaptor.capture());
        final InitiateProsecution actualPayload = payloadArgumentCaptor.getValue();
        assertThat(actualPayload.getCaseDetails().getDateReceived(), is(clock.now().toLocalDate()));
    }

    private <T> Envelope<T> testEnvelope(final T payload, final String eventName, final String submissionId) {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(randomUUID());

        return envelopeFrom(metadataFrom(createObjectBuilder(
                metadataBuilder.build().asJsonObject()).build())
                .withUserId(randomUUID().toString()).build(), payload);
    }

    private <T> Envelope<T> testEnvelope(final T payload, final String eventName, final String submissionId, final ZonedDateTime createdAt) {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(randomUUID())
                .createdAt(createdAt);

        return envelopeFrom(metadataFrom(createObjectBuilder(
                metadataBuilder.build().asJsonObject()).build())
                .withUserId(randomUUID().toString()).build(), payload);
    }
}