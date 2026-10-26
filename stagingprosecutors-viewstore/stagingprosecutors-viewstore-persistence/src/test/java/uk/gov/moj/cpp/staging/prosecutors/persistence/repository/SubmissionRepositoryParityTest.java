package uk.gov.moj.cpp.staging.prosecutors.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * J17/J25 parity characterisation (J17 golden master) for SubmissionRepository.
 *
 * On J17 the repository is a DeltaSpike {@code EntityRepository} that uses the framework's request-scoped
 * EntityManager (EntityManagerProducer). The J25 upgrade replaces it with a hand-written class using a
 * container-managed {@code @PersistenceContext}. Every assertion below encodes the J17 result.
 *
 * Harness note: the J25 harness creates the schema with hbm2ddl. On Hibernate 6 that makes submission.received_at a
 * column with time zone, so the received_at test first changes it back to TIMESTAMP, to match
 * Liquibase ("datetime") and the J17 harness.
 */
public class SubmissionRepositoryParityTest {

    private static final ZoneId LONDON = ZoneId.of("Europe/London");

    @RegisterExtension
    static final HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("stagingprosecutors-test");

    private SubmissionRepository submissionRepository;

    private EntityManager entityManager;

    private TimeZone originalDefaultTimeZone;

    @BeforeEach
    public void setUp() {
        originalDefaultTimeZone = TimeZone.getDefault();
        submissionRepository = new SubmissionRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(submissionRepository);
        entityManager = hibernateTestEntityManagerProvider.getEntityManager();
    }

    @AfterEach
    public void restoreDefaultTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    // SP-P01 (BC-01/02): findBy is the only single-result finder; an unknown id gives null (the query view maps it to 404).
    @Test
    public void spP01_findByUnknownIdShouldReturnNull() {
        assertThat(submissionRepository.findBy(randomUUID()), is(nullValue()));
    }

    // SP-P02: on J17 the framework EntityManagerProducer set the JVM default time zone to UTC when it created the
    // EntityManager the repository used. J25 repositories no longer go through that producer, so using them leaves
    // the JVM default time zone unchanged and the server must run in UTC itself.
    @Test
    public void spP02_repositoryUseShouldLeaveTheJvmDefaultTimeZoneUnchanged() {
        TimeZone.setDefault(TimeZone.getTimeZone(LONDON));

        submissionRepository.save(submission("URN" + randomUUID().toString().substring(0, 8), ZonedDateTime.now(LONDON)));

        assertThat(TimeZone.getDefault().getID(), is("Europe/London"));
    }

    // SP-P02: submission.received_at is a column without time zone (Liquibase "datetime"); J17 stores the UTC
    // wall-clock time even when the JVM default zone is not UTC.
    @Test
    public void spP02_shouldStoreReceivedAtAsUtcWallClock() {
        final String caseUrn = "URN" + randomUUID().toString().substring(0, 8);
        entityManager.createNativeQuery("alter table submission alter column received_at set data type timestamp").executeUpdate();
        TimeZone.setDefault(TimeZone.getTimeZone(LONDON));

        submissionRepository.save(submission(caseUrn, ZonedDateTime.of(2024, 6, 1, 10, 0, 0, 0, LONDON)));
        entityManager.flush();

        final Object storedReceivedAt = entityManager
                .createNativeQuery("select cast(received_at as varchar(64)) from submission where case_urn = ?1")
                .setParameter(1, caseUrn)
                .getSingleResult();
        assertThat(storedReceivedAt, is("2024-06-01 09:00:00"));
    }

    // SP-P03: save() of a new entity returns a managed entity with the same field values. DeltaSpike (J17) returned the
    // same instance; the J25 merge() returns a managed copy. No caller relies on the identity.
    @Test
    public void spP03_saveOfNewEntityShouldReturnAManagedEntityWithTheSameFields() {
        final Submission submission = submission("URN" + randomUUID().toString().substring(0, 8), ZonedDateTime.now(LONDON));

        final Submission saved = submissionRepository.save(submission);

        assertThat(saved, samePropertyValuesAs(submission));
        assertThat(entityManager.contains(saved), is(true));
    }

    private static Submission submission(final String caseUrn, final ZonedDateTime receivedAt) {
        return new Submission(randomUUID(), "PENDING", caseUrn, "AA1234567",
                createArrayBuilder().build(), createArrayBuilder().build(), SubmissionType.PROSECUTION, receivedAt, false, null);
    }
}
