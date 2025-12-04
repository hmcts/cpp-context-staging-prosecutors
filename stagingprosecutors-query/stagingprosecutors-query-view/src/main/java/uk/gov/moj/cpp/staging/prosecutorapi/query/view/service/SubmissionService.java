package uk.gov.moj.cpp.staging.prosecutorapi.query.view.service;

import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.SubmissionRepository;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

public class SubmissionService {
    @Inject
    private SubmissionRepository repository;

    public Optional<Submission> getSubmission(final UUID submissionId) {
        return ofNullable(repository.findBy(submissionId));
    }
}
