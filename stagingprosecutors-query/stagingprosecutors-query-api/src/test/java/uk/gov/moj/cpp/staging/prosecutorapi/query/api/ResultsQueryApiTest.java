package uk.gov.moj.cpp.staging.prosecutorapi.query.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutorapi.query.api.converter.ResultsV1ResponseTransformer;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResultsQueryApiTest {

    @InjectMocks
    private ResultsQueryApi resultsQueryApi;

    @Mock
    private Requester requester;

    @Mock
    private ResultsV1ResponseTransformer resultsV1ResponseTransformer;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @BeforeEach
    public void stubTransformerAsPassthrough() {
        lenient().when(resultsV1ResponseTransformer.transform(any(JsonObject.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    public void shouldInvokeResultsApiWithCorrectEnvelope() {

        final JsonEnvelope resultsResponse = createEnvelope("results.query.api", createObjectBuilder().add("hearingVenue", "response").build());
        when(requester.request(any(Envelope.class))).thenReturn(resultsResponse);

        JsonEnvelope inputEnvelope = getInputEnvelope("ouCode", "2012-02-10", "2012-02-15");
        resultsQueryApi.getResults(inputEnvelope);

        verify(requester).request(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertEnvelope(inputEnvelope, jsonEnvelope);
    }

    @Test
    public void shouldReturnResponseFromResultsApiWithCorrectEnvelope() {

        final JsonEnvelope resultsResponse = createEnvelope("results.query.api", createObjectBuilder().add("hearingVenue", "response").build());
        when(requester.request(any(Envelope.class))).thenReturn(resultsResponse);

        JsonEnvelope inputEnvelope = getInputEnvelope("ouCode", "2012-02-10", "2012-02-15");
        final JsonEnvelope actualResponse = resultsQueryApi.getResults(inputEnvelope);

        verify(requester).request(jsonEnvelopeArgumentCaptor.capture());
        assertThat(actualResponse.metadata().name(), is("hmcts.results.v1"));
        assertThat(actualResponse.payload().toString(), is(resultsResponse.payload().toString()));
    }

    @Test
    public void shouldInvokeResultsApiWithCorrectEnvelopeWithEndDateNull() {

        final JsonEnvelope resultsResponse = createEnvelope("results.query.api", createObjectBuilder().add("hearingVenue", "response").build());
        when(requester.request(any(Envelope.class))).thenReturn(resultsResponse);

        JsonEnvelope inputEnvelope = getInputEnvelope("ouCode", "2012-02-10", null);
        resultsQueryApi.getResults(inputEnvelope);

        verify(requester).request(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertEnvelope(inputEnvelope, jsonEnvelope);
    }

    @Test
    public void shouldCallTransformerForV1Endpoint() {
        final JsonObject responsePayload = createObjectBuilder()
                .add("hearingVenue", createObjectBuilder()
                        .add("courtSessions", javax.json.Json.createArrayBuilder()))
                .build();
        when(requester.request(any(Envelope.class))).thenReturn(
                createEnvelope("results.query.api", responsePayload));

        resultsQueryApi.getResults(getInputEnvelope("ouCode", "2012-02-10", "2012-02-15"));

        verify(resultsV1ResponseTransformer).transform(responsePayload);
    }

    @Test
    public void shouldReturnTransformedPayloadForV1Endpoint() {
        final JsonObject responsePayload = createObjectBuilder()
                .add("hearingVenue", createObjectBuilder()
                        .add("courtSessions", javax.json.Json.createArrayBuilder()))
                .build();
        final JsonObject transformedPayload = createObjectBuilder().add("transformed", true).build();
        when(requester.request(any(Envelope.class))).thenReturn(
                createEnvelope("results.query.api", responsePayload));
        when(resultsV1ResponseTransformer.transform(responsePayload)).thenReturn(transformedPayload);

        final JsonEnvelope response = resultsQueryApi.getResults(getInputEnvelope("ouCode", "2012-02-10", "2012-02-15"));

        assertThat(response.payloadAsJsonObject().getBoolean("transformed"), is(true));
    }

    @Test
    public void shouldReturnFullVerdictObjectFromV2Endpoint() {
        final JsonObject verdictPayload = createObjectBuilder()
                .add("hearingVenue", createObjectBuilder()
                        .add("verdict", createObjectBuilder()
                                .add("verdictCode", "G")
                                .add("verdictDate", "2020-03-12")
                                .add("verdictType", "FOUND_GUILTY")))
                .build();
        when(requester.request(any(Envelope.class))).thenReturn(
                createEnvelope("results.query.api", verdictPayload));

        final JsonEnvelope response = resultsQueryApi.getResultsV2(getInputEnvelope("ouCode", "2012-02-10", "2012-02-15"));

        assertThat(response.metadata().name(), is("hmcts.results.v2"));
        final JsonObject returnedVerdict = response.payloadAsJsonObject()
                .getJsonObject("hearingVenue")
                .getJsonObject("verdict");
        assertThat(returnedVerdict.getString("verdictCode"), is("G"));
        assertThat(returnedVerdict.getString("verdictDate"), is("2020-03-12"));
        assertThat(returnedVerdict.getString("verdictType"), is("FOUND_GUILTY"));
    }

    @Test
    public void shouldNotCallTransformerForV2Endpoint() {
        when(requester.request(any(Envelope.class))).thenReturn(
                createEnvelope("results.query.api", createObjectBuilder().add("hearingVenue", "response").build()));

        resultsQueryApi.getResultsV2(getInputEnvelope("ouCode", "2012-02-10", "2012-02-15"));

        verify(resultsV1ResponseTransformer, never()).transform(any(JsonObject.class));
    }

    @Test
    public void shouldReThrowSameExceptionIfItIsNotRuntimeException() {

        JsonEnvelope inputEnvelope = getInputEnvelope("ouCode", "2012-02-10", "2012-03-10");
        doThrow(new OutOfMemoryError("OutOfMemoryException")).when(requester).request(any(Envelope.class));

        final OutOfMemoryError outOfMemoryError = assertThrows(OutOfMemoryError.class, () -> resultsQueryApi.getResults(inputEnvelope));
        assertThat(outOfMemoryError.getMessage(), is("OutOfMemoryException"));
    }

    private void assertEnvelope(final JsonEnvelope inputEnvelope, final JsonEnvelope jsonEnvelope) {
        assertThat(jsonEnvelope.metadata().name(), is("results.prosecutor-results"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("ouCode"), is(inputEnvelope.payloadAsJsonObject().getString("ouCode")));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("startDate"), is(inputEnvelope.payloadAsJsonObject().getString("startDate")));

        if (inputEnvelope.payloadAsJsonObject().get("endDate") != null) {
            assertThat(jsonEnvelope.payloadAsJsonObject().getString("endDate"), is(inputEnvelope.payloadAsJsonObject().getString("endDate")));
        }
    }

    private JsonEnvelope getInputEnvelope(final String ouCode, final String startDate, final String endDate) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add("ouCode", ouCode)
                .add("startDate", startDate);
        if (endDate != null) {
            builder.add("endDate", endDate);
        }
        return createEnvelope("input.random", builder.build());
    }
}
