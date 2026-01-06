package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;

import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;

import java.io.StringReader;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

/**
 * Integration tests for /stagingprosecutors-query-api/query/api/rest/stagingprosecutors/v1/results/{ouCode}
 */
public class ResultsIT {

    private static final String READ_BASE_RESULTS_URI_V1 = getBaseUri() + "/stagingprosecutors-query-api/query/api/rest/stagingprosecutors/v1/results";
    private static final String MEDIA_TYPE = "application/vnd.hmcts.results.v1+json";
    private static final RestClient restClient = new RestClient();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private WiremockUtils wiremockUtils;

    @BeforeEach
    public void setUp() {
        wiremockUtils = new WiremockUtils().stubIdMapperRecordingNewAssociation().stubGetProsecutionResults("007WZ231");
    }

    @Test
    public void shouldReturnForbiddenAsResponseWhenWrongUserIsPassed() {
        String url = READ_BASE_RESULTS_URI_V1 + "/007WZ231?startDate=\"2020-02-10\"&endDate=\"2020-02-15\"";
        try (Response response = restClient.query(url, MEDIA_TYPE)) {
            assertThat(response.getStatus(), is(HttpStatus.SC_FORBIDDEN));
        }
    }

    @Test
    public void shouldReturnNotFoundAsResponseWhenOUCodeIsNotPassed() {
        String url = READ_BASE_RESULTS_URI_V1 + "?startDate=\"2020-02-10\"&endDate=\"2020-02-15\"";
        try (Response response = restClient.query(url, MEDIA_TYPE, getHeaders())) {
            assertThat(response.getStatus(), is(HttpStatus.SC_NOT_FOUND));
        }
    }

    @Test
    public void shouldReturnBadRequestAsResponseWhenStartDateIsNotPassed() {
        String url = READ_BASE_RESULTS_URI_V1 + "/007WZ231?endDate=\"2020-02-15\"";
        try (Response response = restClient.query(url, MEDIA_TYPE, getHeaders())) {
            assertThat(response.getStatus(), is(HttpStatus.SC_BAD_REQUEST));
        }
    }

    @Test
    public void shouldReturn200WhenResultsQueryApiIsInvokedWithoutEndDate() {
        String url = READ_BASE_RESULTS_URI_V1 + "/007WZ231?startDate=\"2020-02-10\"";
        try (Response response = restClient.query(url, MEDIA_TYPE, getHeaders())) {
            assertThat(response.getStatus(), is(HttpStatus.SC_OK));
            assertThat(response.getMediaType().toString(), is(MEDIA_TYPE));
            assertThat(response.readEntity(String.class), containsString("\"prosecutionAuthorityOuCode\":\"GTL0002\""));
        }
    }

    @Test
    public void shouldReturn200WhenResultsQueryApiIsInvoked() {
        String url = READ_BASE_RESULTS_URI_V1 + "/007WZ231?startDate=\"2020-02-10\"&endDate=\"2020-02-15\"";
        try (Response response = restClient.query(url, MEDIA_TYPE, getHeaders())) {
            assertThat(response.getStatus(), is(HttpStatus.SC_OK));
            assertThat(response.getMediaType().toString(), is(MEDIA_TYPE));
            assertThat(response.readEntity(String.class), containsString("\"prosecutionAuthorityOuCode\":\"GTL0002\""));
        }
    }

    @Test
    public void shouldCascadeErrorsFromResultsApiWhenThereIsAnException() {
        wiremockUtils.stubIdMapperRecordingNewAssociation().stubGetProsecutionResultsForException("CODE_420");
        String url = READ_BASE_RESULTS_URI_V1 + "/CODE_420?startDate=\"2020-02-10\"&endDate=\"2020-02-15\"";

        try (Response response = restClient.query(url, MEDIA_TYPE, getHeaders())) {
            assertThat(response.getStatus(), is(HttpStatus.SC_BAD_REQUEST));
            assertThat(response.getMediaType().toString(), is(MEDIA_TYPE));
            final JsonObject responsePayload = createReader(new StringReader(response.readEntity(String.class))).readObject();
            assertThat(responsePayload.getString("error"), containsString("Error calling results service - Internal Server Error"));
        }
    }

    private MultivaluedMap<String, Object> getHeaders() {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, UUID.randomUUID());

        return headers;
    }

}
