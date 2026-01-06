package uk.gov.moj.cpp.staging.prosecutorapi.it;

import uk.gov.justice.services.messaging.JsonObjects;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.ProsecutionCaseFileApi.expectInitiateSjpProsecutionInvokedWith;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.ResourcesUtils.readResource;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.SJP_CONTENT_TYPE_V2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.importCaseShouldFailV2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.pollForSubmissionV2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.publishPublicProsecutionSubmissionSucceededAndWaitForProcessing;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.SUCCESS;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.SUCCESS_WITH_WARNINGS;
import static uk.gov.moj.cpp.staging.prosecutors.test.utils.JsonObjectsHelper.readFromString;

import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.moj.cpp.staging.prosecutorapi.model.query.v2.Submission;
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
public class SubmitSJPProsecutionV2IT {

    private static final String OUCODE = "GAEAA01";
    private static final String SJP_PROSECUTION_PAYLOAD = "importCase/stagingprosecutors.submit-sjp-prosecution-v2.json";
    private static final String SJP_PROSECUTION_WITHOUT_DEFENDANT_TITLE_PAYLOAD = "importCase/stagingprosecutors.submit-sjp-prosecution-v2-without-defendant-title.json";
    private static final String PROSECUTIONCASEFILE_EXPECTED_PAYLOAD = "importCase/prosecutioncasefile.initiate-sjp-prosecution-v2.json";
    private static final String EVENT_NAME = "stagingprosecutors.event.sjp-prosecution-received";

    @BeforeEach
    public void setup() {
        new WiremockUtils()
                .stubPost("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/initiate-sjp-prosecution")
                .stubIdMapperRecordingNewAssociation();
    }

    @Test
    public void shouldSubmitMultiOffenceProsecutionSuccessfully() {
        final UUID submissionId = createSjpProsecution();
        publishPublicProsecutionSubmissionSucceededAndWaitForProcessing(submissionId);
        final String submissionType = pollForSubmissionV2(submissionId, SUCCESS, OUCODE).getType();
        assertThat(submissionType, is(SubmissionType.PROSECUTION.toString()));
    }
    @Test
    public void shouldSubmitMultiOffenceProsecutionWithoutDefendantTitleSuccessfully() {
        final UUID submissionId = createSjpProsecutionWithoutDefendantTitle();
        publishPublicProsecutionSubmissionSucceededAndWaitForProcessing(submissionId);
        final String submissionType = pollForSubmissionV2(submissionId, SUCCESS, OUCODE).getType();
        assertThat(submissionType, is(SubmissionType.PROSECUTION.toString()));
    }
    @Test
    public void shouldSubmitMultiOffenceProsecutionSuccessfullyWithWarnings() {
        final UUID submissionId = createSjpProsecution();
        StagingProsecutors.publishPublicProsecutionSubmissionSucceededWithWarningsAndWaitForProcessing(submissionId);
        final Submission submission = pollForSubmissionV2(submissionId, SUCCESS_WITH_WARNINGS, OUCODE);
        assertThat(submission.getType(), is(SubmissionType.PROSECUTION.toString()));
    }
    @Test
    public void shouldSubmitOffenceToProsecutioncasefile() {
        createSjpProsecution();
        expectInitiateSjpProsecutionInvokedWith(PROSECUTIONCASEFILE_EXPECTED_PAYLOAD);
    }
    private UUID createSjpProsecution() {
        return StagingProsecutors.importCaseAndWaitUntilReadyV2(SJP_PROSECUTION_PAYLOAD, SJP_CONTENT_TYPE_V2, OUCODE, OUCODE, EVENT_NAME);
    }
    private UUID createSjpProsecutionWithoutDefendantTitle() {
        return StagingProsecutors.importCaseAndWaitUntilReadyV2(SJP_PROSECUTION_WITHOUT_DEFENDANT_TITLE_PAYLOAD, SJP_CONTENT_TYPE_V2,
                OUCODE, OUCODE, EVENT_NAME);
    }

    @Test
    public void shouldSubmitSingleOffenceProsecutionSuccessfully() {
        shouldSubmitProsecutionAndAssertSentPayloadWithSubmissionStatus("stagingprosecutors.submit-sjp-prosecution-v2.json",
                StagingProsecutors::publishPublicProsecutionSubmissionSucceededAndWaitForProcessing,
                "prosecutioncasefile.initiate-sjp-prosecution-v2.json",
                SUCCESS);
    }

    @Test
    public void shouldSubmitSingleOffenceProsecutionSuccessfullyWithWarnings() {
        shouldSubmitProsecutionAndAssertSentPayloadWithSubmissionStatus("stagingprosecutors.submit-sjp-prosecution-v2.json",
                StagingProsecutors::publishPublicProsecutionSubmissionSucceededWithWarningsAndWaitForProcessing,
                "prosecutioncasefile.initiate-sjp-prosecution-v2.json",
                SUCCESS_WITH_WARNINGS);
    }

    @Test
    public void shouldSubmitProsecutionFailWhenURN_is_Bad() {
        importCaseShouldFailV2("importCase/stagingprosecutors.submit-sjp-prosecution_with_wrong_urn.json");
    }

    @Test
    public void shouldSubmitProsecutionFailWhenProsecutorDefendantId_is_Bad() {
        importCaseShouldFailV2("importCase/stagingprosecutors.submit-sjp-prosecution_with_wrong_pid.json");
    }

    private void shouldSubmitProsecutionAndAssertSentPayloadWithSubmissionStatus(final String submitSjpProsecutionFileName, final Consumer<UUID> publishPublicProsecutionSubmissionSucceededConsumer,
                                                                                 final String initiateSjpProsecutionFileName, final SubmissionStatus status) {
        final UUID submissionId = StagingProsecutors.importCaseAndWaitUntilReadyV2SJP(format("importCase/%s", submitSjpProsecutionFileName), SJP_CONTENT_TYPE_V2, OUCODE, OUCODE);

        final String pcfFile = readResource(format("importCase/%s", initiateSjpProsecutionFileName)).replace("PROSECUTING_AUTHORITY", OUCODE);
        expectInitiateSjpProsecutionInvokedWith(readFromString(pcfFile));

        publishPublicProsecutionSubmissionSucceededConsumer.accept(submissionId);

        final Submission updatedSubmission = pollForSubmissionV2(submissionId, status, OUCODE);
        assertThat(updatedSubmission.getType(), is(SubmissionType.PROSECUTION.toString()));
    }
}
