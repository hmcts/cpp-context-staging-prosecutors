package uk.gov.moj.cpp.staging.prosecutors.persistence.repository;

import static java.time.ZonedDateTime.now;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(CdiTestRunner.class)
public class SubmissionRepositoryTest extends BaseTransactionalJunit4Test {

    @Inject
    private SubmissionRepository submissionRepository;

    @Test
    public void shouldSaveSubmission() {

        final UUID submissionId = UUID.randomUUID();

        final SubmissionType type = randomEnum(SubmissionType.class).next();
        final Submission submission = new Submission(
                submissionId,
                STRING.next(),
                "caseUrn",
                "AA1234567",
                createArrayBuilder().build(),
                createArrayBuilder().build(),
                type,
                now(),
                false,
                null);

        submission.setCompletedAt(now());
        submission.setCaseWarnings(createArrayBuilder().add(
                createObjectBuilder().add("caseWarnings", "caseWarning").build())
                .build());
        submission.setDefendantWarnings(createArrayBuilder().add(
                createObjectBuilder().add("defendantWarnings", "defendantWarning").build())
                .build());
        submissionRepository.save(submission);

        final Submission submissionFind = submissionRepository.findBy(submissionId);

        assertThat(submissionFind, not(nullValue()));

        assertThat(submissionFind.getSubmissionId(), is(submission.getSubmissionId()));
        assertThat(submissionFind.getSubmissionStatus(), is(submission.getSubmissionStatus()));
        assertThat(submissionFind.getCaseUrn(), is(submission.getCaseUrn()));
        assertThat(submissionFind.getOuCode(), is(submission.getOuCode()));
        assertThat(submissionFind.getErrors(), is(submission.getErrors()));
        assertThat(submissionFind.getWarnings(), is(submission.getWarnings()));
        assertThat(submissionFind.getType(), is(type));
        assertThat(submissionFind.getReceivedAt(), is(submission.getReceivedAt()));
        assertThat(submissionFind.getCompletedAt(), is(submission.getCompletedAt()));
        assertThat(submissionFind.getCaseWarnings(), is(submission.getCaseWarnings()));
        assertThat(submissionFind.getDefendantWarnings(), is(submission.getDefendantWarnings()));
        assertThat(submissionFind.getCpsCase(), is(submission.getCpsCase()));

        submission.setCpsCase(false);
        assertThat(submission.isCpsCase(), is(false));

        submission.setCpsCase(true);
        assertThat(submission.isCpsCase(), is(true));

        submission.setCpsCase(null);
        assertThat(submission.isCpsCase(), is(false));
    }
}
