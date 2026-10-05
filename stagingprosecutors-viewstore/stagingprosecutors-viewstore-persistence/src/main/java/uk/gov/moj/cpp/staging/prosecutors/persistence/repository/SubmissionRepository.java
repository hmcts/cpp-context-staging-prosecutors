package uk.gov.moj.cpp.staging.prosecutors.persistence.repository;


import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class SubmissionRepository {

    @PersistenceContext(unitName = "stagingprosecutors")
    EntityManager entityManager;

    public Submission findBy(final UUID submissionId) {
        return entityManager.find(Submission.class, submissionId);
    }

    public Submission save(final Submission submission) {
        return entityManager.merge(submission);
    }
}
