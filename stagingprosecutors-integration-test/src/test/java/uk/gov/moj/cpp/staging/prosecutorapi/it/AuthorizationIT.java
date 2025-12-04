package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.google.common.io.Resources.getResource;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.CHARGE_CONTENT_TYPE_V2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.DIFFERENT_OUCODE;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.OUCODE;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.OUCODE_LOWER_CASE;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.REQUISITION_CONTENT_TYPE_V2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.SJP_CONTENT_TYPE_V2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.SUMMONS_CONTENT_TYPE_V2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.postCommandV2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.sendFileUploadRequestV2;

import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.ProsecutionCaseFileApi;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;

import java.io.File;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(JmsResourceManagementExtension.class)
public class AuthorizationIT {

    private static void stubPCFcommand(final UUID userId) {
        stubFor(post(urlPathMatching("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/*"))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, userId.toString())
                        .withHeader(CONTENT_TYPE, "application/json")));

    }

    @BeforeEach
    public void setUpStub() {
        new WiremockUtils()
                .stubPost("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/cc-prosecution")
                .stubIdMapperRecordingNewAssociation();
    }

    @Test
    public void shouldReturn403WhenOuCodeMismatchForSummonsCase() {
        try (Response response = postCommandV2("importCase/nonsjp/summons/stagingprosecutors.submit-summons-prosecution-fail-fast-errors.json", SUMMONS_CONTENT_TYPE_V2, OUCODE, DIFFERENT_OUCODE)) {
            assertThat(response.getStatus(), is(HttpStatus.SC_FORBIDDEN));
        }
    }

    @Test
    public void shouldReturn403WhenOuCodeMismatchForChargeCase() {
        try (Response response = postCommandV2("importCase/nonsjp/charge/stagingprosecutors.submit-charge-fail-fast-errors.json", CHARGE_CONTENT_TYPE_V2, OUCODE, DIFFERENT_OUCODE)) {
            assertThat(response.getStatus(), is(HttpStatus.SC_FORBIDDEN));
        }
    }

    @Test
    public void shouldReturn403WhenOuCodeMismatchForRequisitionCase() {
        try (Response response = postCommandV2("importCase/nonsjp/requisition/stagingprosecutors.submit-requisition-prosecution-fail-fast-errors.json", REQUISITION_CONTENT_TYPE_V2, OUCODE, DIFFERENT_OUCODE)) {
            assertThat(response.getStatus(), is(HttpStatus.SC_FORBIDDEN));
        }
    }

    @Test
    public void shouldReturn403WhenOuCodeMismatchForSJPCase() {
        try (Response response = postCommandV2("importCase/stagingprosecutors.submit-sjp-prosecution.json", SJP_CONTENT_TYPE_V2, OUCODE, DIFFERENT_OUCODE)) {
            assertThat(response.getStatus(), is(HttpStatus.SC_FORBIDDEN));
        }
    }

    @Test
    public void shouldReturn403WhenOuCodeMismatchForSubmittingMaterial() throws Exception {
        final String caseUrn = STRING.next();

        final HttpResponse response = sendFileUploadRequestV2(caseUrn,
                new File(getResource("submitProsecutionDocument/41b2Y9DaQ3L.jpg").getFile()),
                "SJPN", OUCODE, DIFFERENT_OUCODE);

        assertThat(response.getStatusLine().getStatusCode(), is(HttpStatus.SC_FORBIDDEN));
    }

    @Test
    public void shouldReturn403WhenOuCodeMismatchForQuerySubmission() {
        stubPCFcommand(randomUUID());
        final UUID submissionId = StagingProsecutors.importCaseAndWaitUntilReadyV2("importCase/nonsjp/charge/stagingprosecutors.submit-charge-individual-all.json", CHARGE_CONTENT_TYPE_V2, OUCODE, OUCODE);

        ProsecutionCaseFileApi.expectInitiateProsecutionInvokedWith("importCase/nonsjp/charge/prosecutioncasefile.initiate-cc-prosecution-charge-all.json");

        StagingProsecutors.publishPublicProsecutionSubmissionRejectedAndWaitForProcessing(submissionId);

        try (Response response = StagingProsecutors.pollQuerySubmissionV2(submissionId, DIFFERENT_OUCODE)) {
            assertThat(response.getStatus(), is(HttpStatus.SC_FORBIDDEN));
        }
    }

    @Test
    public void shouldReturn200WhenOuCodeLowerCaseForQuerySubmission() {
        stubPCFcommand(randomUUID());
        final UUID submissionId = StagingProsecutors.importCaseAndWaitUntilReadyV2("importCase/nonsjp/charge/stagingprosecutors.submit-charge-individual-all.json", CHARGE_CONTENT_TYPE_V2, OUCODE, OUCODE);

        try (Response response = StagingProsecutors.pollQuerySubmissionV2(submissionId, OUCODE_LOWER_CASE)) {
            assertThat(response.getStatus(), is(HttpStatus.SC_OK));
        }
    }

}
