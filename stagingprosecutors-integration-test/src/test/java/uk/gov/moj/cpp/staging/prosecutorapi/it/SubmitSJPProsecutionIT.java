package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.ProsecutionCaseFileApi.expectInitiateSjpProsecutionInvokedWith;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.importCaseShouldFail;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.pollForSubmission;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.publishPublicProsecutionSubmissionSucceededAndWaitForProcessing;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.publishPublicProsecutionSubmissionSucceededWithWarningsAndWaitForProcessing;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.SUCCESS;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.SUCCESS_WITH_WARNINGS;

import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.moj.cpp.staging.prosecutorapi.model.query.Submission;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType;

import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public class SubmitSJPProsecutionIT {

    @BeforeEach
    public void setUpStub() {
        new WiremockUtils()
                .stubPost("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/initiate-sjp-prosecution")
                .stubIdMapperRecordingNewAssociation();
    }

    @Test
    public void shouldSubmitSingleOffenceProsecutionSuccessfully() {
        shouldSubmitProsecutionAndAssertSentPayloadWithSubmissionStatus("stagingprosecutors.submit-sjp-prosecution.json",
                (submissionId) -> publishPublicProsecutionSubmissionSucceededAndWaitForProcessing(submissionId),
                "prosecutioncasefile.initiate-sjp-prosecution.json",
                SUCCESS);
    }

    @Test
    public void shouldSubmitProsecutionWithoutDefendantTitleSuccessfully() {
        shouldSubmitProsecutionAndAssertSentPayloadWithSubmissionStatus("stagingprosecutors.submit-sjp-prosecution-without-defendant-title.json",
                (submissionId) -> publishPublicProsecutionSubmissionSucceededAndWaitForProcessing(submissionId),
                "prosecutioncasefile.initiate-sjp-prosecution-without-defendant-title.json",
                SUCCESS);
    }

    @Test
    public void shouldSubmitProsecutionWithoutPostcodeSuccessfully() {
        shouldSubmitProsecutionAndAssertSentPayloadWithSubmissionStatus("stagingprosecutors.submit-sjp-prosecution-without-postcode.json",
                (submissionId) -> publishPublicProsecutionSubmissionSucceededAndWaitForProcessing(submissionId),
                "prosecutioncasefile.initiate-sjp-prosecution-without-postcode.json",
                SUCCESS);
    }

    @Test
    public void shouldSubmitSingleOffenceProsecutionSuccessfullyWithWarnings() {
        shouldSubmitProsecutionAndAssertSentPayloadWithSubmissionStatus("stagingprosecutors.submit-sjp-prosecution.json",
                (submissionId) -> publishPublicProsecutionSubmissionSucceededWithWarningsAndWaitForProcessing(submissionId),
                "prosecutioncasefile.initiate-sjp-prosecution.json",
                SUCCESS_WITH_WARNINGS);
    }


    @Test
    public void shouldSubmitProsecutionFailWhenURN_is_Bad() {
        importCaseShouldFail("importCase/stagingprosecutors.submit-sjp-prosecution_with_wrong_urn.json");
    }

    @Test
    public void shouldSubmitProsecutionFailWhenProsecutorDefendantId_is_Bad() {
        importCaseShouldFail("importCase/stagingprosecutors.submit-sjp-prosecution_with_wrong_pid.json");
    }

    private void shouldSubmitProsecutionAndAssertSentPayloadWithSubmissionStatus(final String submitSjpProsecutionFileName, final Consumer<UUID> publishPublicProsecutionSubmissionSucceededConsumer,
                                                                                 final String initiateSjpProsecutionFileName, final SubmissionStatus status){
        final UUID submissionId = StagingProsecutors.importCaseAndWaitUntilReady(format("importCase/%s", submitSjpProsecutionFileName));
        pollForSubmission(submissionId, PENDING);

        expectInitiateSjpProsecutionInvokedWith(format("importCase/%s", initiateSjpProsecutionFileName));

        publishPublicProsecutionSubmissionSucceededConsumer.accept(submissionId);

        final Submission updatedSubmission = pollForSubmission(submissionId, status);
        assertThat(updatedSubmission.getType(), is(SubmissionType.PROSECUTION.toString()));
    }
}
