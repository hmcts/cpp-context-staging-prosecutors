package uk.gov.moj.cpp.staging.prosecutorapi.query.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

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

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;


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
    public void shouldReThrowSameExceptionIfItIsNotRuntimeException() {

        JsonEnvelope inputEnvelope = getInputEnvelope("ouCode", "2012-02-10", "2012-03-10");
        doThrow(new OutOfMemoryError("OutOfMemoryException")).when(requester).request(any(Envelope.class));

        final OutOfMemoryError outOfMemoryError = assertThrows(OutOfMemoryError.class, () -> resultsQueryApi.getResults(inputEnvelope));
        assertThat(outOfMemoryError.getMessage(), is("OutOfMemoryException"));
    }

    @Test
    void shouldFlattenVerdictObjectToVerdictCodeInV1Response() {
        final JsonObject payload = buildPayloadWithOffence(
                Json.createObjectBuilder()
                        .add("offenceCode", "PS90010")
                        .add("pleaValue", "NOT_GUILTY")
                        .add("verdict", Json.createObjectBuilder()
                                .add("verdictCode", "N")
                                .add("verdictType", "FOUND_NOT_GUILTY")
                                .add("verdictDate", "2020-01-20")
                                .build())
                        .build());
        when(requester.request(any(Envelope.class))).thenReturn(createEnvelope("results.query.api", payload));

        final String response = resultsQueryApi.getResults(getInputEnvelope("ouCode", "2020-02-10", "2020-02-15"))
                .payload().toString();

        assertThat(response, containsString("\"verdictCode\":\"N\""));
        assertThat(response, not(containsString("\"verdict\":")));
        assertThat(response, not(containsString("\"verdictType\":")));
        assertThat(response, not(containsString("\"verdictDate\":")));
    }

    @Test
    void shouldLeaveOffenceUnchangedWhenNoVerdictObjectPresent() {
        final JsonObject payload = buildPayloadWithOffence(
                Json.createObjectBuilder()
                        .add("offenceCode", "PS90010")
                        .add("pleaValue", "GUILTY")
                        .build());
        when(requester.request(any(Envelope.class))).thenReturn(createEnvelope("results.query.api", payload));

        final String response = resultsQueryApi.getResults(getInputEnvelope("ouCode", "2020-02-10", "2020-02-15"))
                .payload().toString();

        assertThat(response, not(containsString("\"verdictCode\":")));
        assertThat(response, not(containsString("\"verdict\":")));
        assertThat(response, containsString("\"pleaValue\":\"GUILTY\""));
    }

    @Test
    void shouldPassThroughPayloadUnchangedWhenNoHearingVenueStructurePresent() {
        final JsonObject payload = Json.createObjectBuilder()
                .add("prosecutionAuthorityOuCode", "GTL0002")
                .build();
        when(requester.request(any(Envelope.class))).thenReturn(createEnvelope("results.query.api", payload));

        final String response = resultsQueryApi.getResults(getInputEnvelope("ouCode", "2020-02-10", "2020-02-15"))
                .payload().toString();

        assertThat(response, containsString("\"prosecutionAuthorityOuCode\":\"GTL0002\""));
        assertThat(response, not(containsString("\"verdictCode\":")));
    }

    private JsonObject buildPayloadWithOffence(final JsonObject offence) {
        return Json.createObjectBuilder()
                .add("hearingVenue", Json.createObjectBuilder()
                        .add("courtSessions", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("defendants", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                        .add("prosecutionCasesOrApplications", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("offences", Json.createArrayBuilder()
                                                                                .add(offence)
                                                                                .build())
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
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
