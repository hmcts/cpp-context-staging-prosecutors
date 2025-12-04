package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.staging.prosecutorapi.model.command.SjpProsecutionSubmissionClient;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Defendant;
import uk.gov.moj.cpp.staging.prosecutorapi.model.event.SjpProsecutionRejection;
import uk.gov.moj.cpp.staging.prosecutorapi.model.query.Submission;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProcessSjpSubmissionRejectionIT {

    private String submissionId;

    @BeforeEach
    public void setUpStub() {
        new WiremockUtils()
                .stubPost("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/initiate-sjp-prosecution")
                .stubIdMapperRecordingNewAssociation();
    }

    @Test
    public void shouldUpdateViewStoreWhenSubmissionRejected() {
        final SjpProsecutionSubmissionClient submitProsecution = SjpProsecutionSubmissionClient.builder()
                .defendant(Defendant.builder()
                        .organisation(null)
                        .build())
                .build();
        submitProsecution.caseReceivedHandler = envelope -> submissionId = envelope.payloadAsJsonObject().getString("submissionId");
        submitProsecution.getExecutor().executeSync();

        //check pending submission created
        final Submission interimResponseCheck = Submission
                .poller()
                .setPathParameter("submissionId", submissionId)
                .pollUntil(s -> "PENDING".equals(s.getSubmissionStatus()));
        assertThat(interimResponseCheck.getSubmissionId().toString(), is(submissionId));

        final SjpProsecutionRejection event = SjpProsecutionRejection.builder().externalId(submissionId).build();
        event.emitter().emit();

        //check submission updated with rejected status
        final Submission finalResponseCheck = Submission
                .poller()
                .setPathParameter("submissionId", submissionId)
                .pollUntil(s -> "REJECTED".equals(s.getSubmissionStatus()));

        assertThat(finalResponseCheck.getSubmissionId().toString(), is(submissionId));
    }

}
