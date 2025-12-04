package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.OUCODE;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.SUMMONS_CONTENT_TYPE_V2;

import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.moj.cpp.staging.prosecutorapi.model.query.Submission;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.ProsecutionCaseFileApi;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public class SubmitSummonsProsecutionIT {

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
    public void shouldSubmitProsecutionWithIndividualDefendantParentGuardianIndividualAndIndividualDefendantParentGuardianOrganisationAndOrganisationDefendantAllFieldsAndSetStatusToSuccess() {
        stubPCFcommand(randomUUID());

        final UUID submissionId = StagingProsecutors.importCaseAndWaitUntilReadyV2("importCase/nonsjp/summons/stagingprosecutors.submit-summons-prosecution-all-fields.json", SUMMONS_CONTENT_TYPE_V2, OUCODE, OUCODE);

        ProsecutionCaseFileApi.expectInitiateProsecutionInvokedWith("importCase/nonsjp/summons/prosecutioncasefile.initiate-summons-prosecution-all-fields.json");

        StagingProsecutors.publishPublicProsecutionSubmissionSucceededAndWaitForProcessing(submissionId);

        final Submission updatedSubmission = StagingProsecutors.pollForSubmission(submissionId, SubmissionStatus.SUCCESS);
        assertThat(updatedSubmission.getType(), is(SubmissionType.PROSECUTION.toString()));
    }

    @Test
    public void shouldSubmitProsecutionWithIndividualDefendantAndOrganisationDefendantMandatoryFieldsAndSetStatusToSuccess() {
        stubPCFcommand(randomUUID());

        final UUID submissionId = StagingProsecutors.importCaseAndWaitUntilReadyV2("importCase/nonsjp/summons/stagingprosecutors.submit-summons-prosecution-mandatory-fields.json", SUMMONS_CONTENT_TYPE_V2, OUCODE, OUCODE);

        ProsecutionCaseFileApi.expectInitiateProsecutionInvokedWith("importCase/nonsjp/summons/prosecutioncasefile.initiate-summons-prosecution-mandatory-fields.json");

        StagingProsecutors.publishPublicProsecutionSubmissionSucceededAndWaitForProcessing(submissionId);

        final Submission updatedSubmission = StagingProsecutors.pollForSubmission(submissionId, SubmissionStatus.SUCCESS);
        assertThat(updatedSubmission.getType(), is(SubmissionType.PROSECUTION.toString()));
    }
}
