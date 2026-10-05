package uk.gov.moj.cpp.staging.prosecutors.persistence.repository;

import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SubmissionRepositoryTest {

    private static final String PERSISTENCE_UNIT = "stagingprosecutors-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private SubmissionRepository submissionRepository;

    @BeforeEach
    void createRepository() {
        submissionRepository = new SubmissionRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(submissionRepository);
    }

    @Test
    void shouldSaveSubmission() {

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
                now().truncatedTo(SECONDS),
                false,
                null);

        submission.setCompletedAt(now().truncatedTo(SECONDS));
        submission.setCaseWarnings(createArrayBuilder().add(
                createObjectBuilder().add("caseWarnings", "caseWarning").build())
                .build());
        submission.setDefendantWarnings(createArrayBuilder().add(
                createObjectBuilder().add("defendantWarnings", "defendantWarning").build())
                .build());
        submissionRepository.save(submission);
        flushAndClear();

        final Submission submissionFind = submissionRepository.findBy(submissionId);

        assertThat(submissionFind, not(nullValue()));

        assertThat(submissionFind.getSubmissionId(), is(submission.getSubmissionId()));
        assertThat(submissionFind.getSubmissionStatus(), is(submission.getSubmissionStatus()));
        assertThat(submissionFind.getCaseUrn(), is(submission.getCaseUrn()));
        assertThat(submissionFind.getOuCode(), is(submission.getOuCode()));
        assertThat(submissionFind.getErrors(), is(submission.getErrors()));
        assertThat(submissionFind.getWarnings(), is(submission.getWarnings()));
        assertThat(submissionFind.getType(), is(type));
        assertThat(submissionFind.getReceivedAt().toInstant(), is(submission.getReceivedAt().toInstant()));
        assertThat(submissionFind.getCompletedAt().toInstant(), is(submission.getCompletedAt().toInstant()));
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

    @Test
    void shouldUpdateExistingSubmissionOnSave() {
        final UUID submissionId = UUID.randomUUID();
        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "caseUrn",
                "AA1234567",
                createArrayBuilder().build(),
                createArrayBuilder().build(),
                randomEnum(SubmissionType.class).next(),
                now().truncatedTo(SECONDS),
                false,
                null);
        submissionRepository.save(submission);
        flushAndClear();

        final Submission existing = submissionRepository.findBy(submissionId);
        existing.setSubmissionStatus("SUCCESS");
        submissionRepository.save(existing);
        flushAndClear();

        assertThat(submissionRepository.findBy(submissionId).getSubmissionStatus(), is("SUCCESS"));
    }

    @Test
    void shouldReturnNullWhenNoSubmissionExistsForId() {
        assertThat(submissionRepository.findBy(UUID.randomUUID()), is(nullValue()));
    }

    private void flushAndClear() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        entityManager.flush();
        entityManager.clear();
    }
}
