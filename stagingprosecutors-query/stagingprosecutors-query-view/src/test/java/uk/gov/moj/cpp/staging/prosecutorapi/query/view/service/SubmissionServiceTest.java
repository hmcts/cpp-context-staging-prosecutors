package uk.gov.moj.cpp.staging.prosecutorapi.query.view.service;

import static java.time.ZonedDateTime.now;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;

import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.SubmissionRepository;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubmissionServiceTest {
    @InjectMocks
    private SubmissionService service;

    @Mock
    private SubmissionRepository repository;

    @Test
    public void getOptionalSubmissionByIdIfReturnedByRepository() {
        final UUID submissionId = UUID.randomUUID();
        final SubmissionType submissionType = randomEnum(SubmissionType.class).next();

        final Submission expectedSubmission = new Submission(
                submissionId,
                "PENDING",
                "caseUrn",
                "ouCODE",
                createArrayBuilder().build(),
                createArrayBuilder().build(),
                submissionType,
                now(),
                false,
                null);
        when(repository.findBy(submissionId)).thenReturn(expectedSubmission);

        Optional<Submission> submissionOptional = service.getSubmission(submissionId);

        assertTrue(submissionOptional.isPresent(),
                "The returned value from the service should be present as repository returned it.");
        assertThat("The returned value from the service should be equal to the one returned by repository.",
                submissionOptional.get(), equalTo(expectedSubmission));
    }

    @Test
    public void getEmptyIfSubmissionNotReturnedByRepository() {
        final UUID submissionId = UUID.randomUUID();

        when(repository.findBy(submissionId)).thenReturn(null);

        Optional<Submission> submissionOptional = service.getSubmission(submissionId);

        assertFalse(submissionOptional.isPresent(),
                "The returned value from the service should be empty as repository hasn't returned anything.");
    }

}