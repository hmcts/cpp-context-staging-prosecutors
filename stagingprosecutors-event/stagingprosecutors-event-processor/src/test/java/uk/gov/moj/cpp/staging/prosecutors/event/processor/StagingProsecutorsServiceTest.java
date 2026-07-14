package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsServiceTest {

    private static final String SUBMISSION_ID = randomUUID().toString();
    private static final String QUERY_SUBMISSION_DETAILS = "hmcts.cjs.submission";

    @InjectMocks
    private StagingProsecutorsService stagingProsecutorsService;

    @Mock
    private Requester requester;

    @Mock
    private Enveloper enveloper;

    @Test
    public void shouldReturnSubmissionDetails_whenSubmissionExists() {
        final JsonEnvelope inboundEnvelope = envelopeFrom(
                Envelope.metadataBuilder().withId(randomUUID()).withName("some.event").build(),
                createObjectBuilder().build());

        final JsonObject responsePayload = createObjectBuilder().add("submissionId", SUBMISSION_ID).build();
        final JsonEnvelope responseEnvelope = envelopeFrom(
                Envelope.metadataBuilder().withId(randomUUID()).withName(QUERY_SUBMISSION_DETAILS).build(),
                responsePayload);

        final Function<Object, JsonEnvelope> enveloperFunction = payload -> responseEnvelope;
        when(enveloper.withMetadataFrom(inboundEnvelope, QUERY_SUBMISSION_DETAILS)).thenReturn(enveloperFunction);
        when(requester.request(responseEnvelope)).thenReturn(responseEnvelope);

        final Optional<JsonObject> submissionDetails = stagingProsecutorsService.submissionExistsById(inboundEnvelope, SUBMISSION_ID);

        assertThat(submissionDetails, notNullValue());
        assertThat(submissionDetails.isPresent(), is(true));
        assertThat(submissionDetails.get().getString("submissionId"), is(SUBMISSION_ID));
    }

    @Test
    public void shouldReturnEmptyOptional_whenRequesterThrowsException() {
        final JsonEnvelope inboundEnvelope = envelopeFrom(
                Envelope.metadataBuilder().withId(randomUUID()).withName("some.event").build(),
                createObjectBuilder().build());

        when(enveloper.withMetadataFrom(any(JsonEnvelope.class), any(String.class)))
                .thenReturn(payload -> inboundEnvelope);
        when(requester.request(any(JsonEnvelope.class))).thenThrow(new RuntimeException("query failed"));

        final Optional<JsonObject> submissionDetails = stagingProsecutorsService.submissionExistsById(inboundEnvelope, SUBMISSION_ID);

        assertThat(submissionDetails.isPresent(), is(false));
    }
}
