package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.CHARGE_CONTENT_TYPE_V2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.OUCODE;

import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * J17/J25 parity characterisation (J17 golden master) against the deployed service.
 *
 * SP-I01 (BC-08, SP-P04): the v1 and v2 submission queries return receivedAt / completedAt as ZonedDateTime.toString()
 * of the entity read back from PostgreSQL, so the zone suffix is part of the response contract. Hibernate 6 can change
 * the zone identity of the value it reads back.
 *
 * SP-I02 (BC-16): the framework metrics endpoints under /internal/metrics.
 */
@ExtendWith(JmsResourceManagementExtension.class)
public class JavaUpgradeParityIT {

    private static final String SUBMISSION_V1_URL = getBaseUri() + "/stagingprosecutors-query-api/query/api/rest/stagingprosecutors/v1/submissions/";
    private static final String SUBMISSION_V2_URL = getBaseUri() + "/stagingprosecutors-query-api/query/api/rest/stagingprosecutors/v2/submissions/";
    private static final String METRICS_PING_URL = getBaseUri() + "/stagingprosecutors-service/internal/metrics/ping";

    // ZonedDateTime.toString() of a UTC-region zone, e.g. 2026-10-19T11:52:03.123456Z[UTC]
    private static final String UTC_REGION_TIMESTAMP = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?Z\\[UTC]";

    @BeforeEach
    public void setUp() {
        new WiremockUtils()
                .stubPost("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/cc-prosecution")
                .stubIdMapperRecordingNewAssociation();
        stubFor(post(urlPathMatching("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/*"))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, "application/json")));
    }

    @Test
    public void spI01_submissionQueriesShouldRenderTimestampsInTheJ17Format() {
        final UUID submissionId = StagingProsecutors.importCaseAndWaitUntilReadyV2(
                "importCase/nonsjp/charge/stagingprosecutors.submit-charge-individual-mandatory.json", CHARGE_CONTENT_TYPE_V2, OUCODE, OUCODE);
        StagingProsecutors.publishPublicProsecutionSubmissionSucceededAndWaitForProcessing(submissionId);

        final JsonObject v1 = pollUntilCompleted(SUBMISSION_V1_URL + submissionId, "application/vnd.hmcts.cjs.submission+json");
        assertThat(v1.getString("receivedAt"), matchesPattern(UTC_REGION_TIMESTAMP));
        assertThat(v1.getString("completedAt"), matchesPattern(UTC_REGION_TIMESTAMP));

        final JsonObject v2 = pollUntilCompleted(SUBMISSION_V2_URL + submissionId + "/" + OUCODE, "application/vnd.hmcts.cjs.submission.v2+json");
        assertThat(v2.getString("receivedAt"), matchesPattern(UTC_REGION_TIMESTAMP));
        assertThat(v2.getString("completedAt"), matchesPattern(UTC_REGION_TIMESTAMP));
    }

    @Test
    public void spI02_metricsPingShouldAnswerWithTheJ17Status() throws IOException, InterruptedException {
        final HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(METRICS_PING_URL)).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode(), is(200));
    }

    private static JsonObject pollUntilCompleted(final String url, final String mediaType) {
        final String responsePayload = poll(requestParams(url, mediaType).withHeader(USER_ID, randomUUID()).build())
                .pollDelay(0, MILLISECONDS)
                .pollInterval(100, MILLISECONDS)
                .timeout(20, SECONDS)
                .until(status().is(OK), payload().isJson(withJsonPath("$.completedAt")))
                .getPayload();
        try (JsonReader reader = Json.createReader(new StringReader(responsePayload))) {
            return reader.readObject();
        }
    }
}
