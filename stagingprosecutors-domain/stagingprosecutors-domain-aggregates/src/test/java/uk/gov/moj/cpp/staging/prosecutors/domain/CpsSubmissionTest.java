package uk.gov.moj.cpp.staging.prosecutors.domain;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeBcmReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePtphReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsUpdateCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProblemValue;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatusUpdated;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CpsSubmissionTest {

    @InjectMocks
    private CpsSubmission cpsSubmission;

    @Test
    public void shouldRaiseCpsServePetReceivedEvent() {

        final CpsServePetReceived cpsServePetReceived = CpsServePetReceived.cpsServePetReceived()
                .withSubmissionId(randomUUID())
                .build();

        final Stream<Object> eventStream = cpsSubmission.receivePetSubmission(cpsServePetReceived);

        final CpsServePetReceived receivedCpsServePetReceived = (CpsServePetReceived) eventStream.findFirst().get();
        assertThat(receivedCpsServePetReceived.getSubmissionId(), is(cpsServePetReceived.getSubmissionId()));
        assertThat(receivedCpsServePetReceived.getSubmissionStatus(), is(PENDING));
    }

    @Test
    public void shouldRaiseCpsServeCotrReceivedEvent() {

        final CpsServeCotrReceived cpsServeCotrReceived = CpsServeCotrReceived.cpsServeCotrReceived()
                .withSubmissionId(randomUUID())
                .build();

        final Stream<Object> eventStream = cpsSubmission.receiveCotrSubmission(cpsServeCotrReceived);

        final CpsServeCotrReceived receivedCpsServeCotrReceived = (CpsServeCotrReceived) eventStream.findFirst().get();
        assertThat(receivedCpsServeCotrReceived.getSubmissionId(), is(cpsServeCotrReceived.getSubmissionId()));
        assertThat(receivedCpsServeCotrReceived.getSubmissionStatus(), is(PENDING));
    }

    @Test
    public void shouldRaiseCpsServePtphReceivedEvent() {

        final CpsServePtphReceived cpsServePtphReceived = CpsServePtphReceived.cpsServePtphReceived()
                .withSubmissionId(randomUUID())
                .build();

        final Stream<Object> eventStream = cpsSubmission.receivePtphSubmission(cpsServePtphReceived);

        final CpsServePtphReceived receivedCpsServePtphReceived = (CpsServePtphReceived) eventStream.findFirst().get();
        assertThat(receivedCpsServePtphReceived.getSubmissionId(), is(cpsServePtphReceived.getSubmissionId()));
        assertThat(receivedCpsServePtphReceived.getSubmissionStatus(), is(PENDING));
    }

    @Test
    public void shouldRaiseCpsServeBcmReceivedEvent() {

        final CpsServeBcmReceived cpsServeBcmReceived = CpsServeBcmReceived.cpsServeBcmReceived()
                .withSubmissionId(randomUUID())
                .build();

        final Stream<Object> eventStream = cpsSubmission.receiveBcmSubmission(cpsServeBcmReceived);

        final CpsServeBcmReceived receivedCpsServeBcmReceived = (CpsServeBcmReceived) eventStream.findFirst().get();
        assertThat(receivedCpsServeBcmReceived.getSubmissionId(), is(cpsServeBcmReceived.getSubmissionId()));
        assertThat(receivedCpsServeBcmReceived.getSubmissionStatus(), is(PENDING));
    }

    @Test
    public void shouldRaiseCpsUpdateCotrReceivedEvent() {

        final CpsUpdateCotrReceived cpsUpdateCotrReceived = CpsUpdateCotrReceived.cpsUpdateCotrReceived()
                .withSubmissionId(randomUUID())
                .build();

        final Stream<Object> eventStream = cpsSubmission.receiveUpdateCotrSubmission(cpsUpdateCotrReceived);

        final CpsUpdateCotrReceived receivedCpsUpdateCotrReceived = (CpsUpdateCotrReceived) eventStream.findFirst().get();
        assertThat(receivedCpsUpdateCotrReceived.getSubmissionId(), is(cpsUpdateCotrReceived.getSubmissionId()));
        assertThat(receivedCpsUpdateCotrReceived.getSubmissionStatus(), is(PENDING));
    }

    @Test
    public void shouldRaiseSubmissionStatusUpdatedEvent() {

        final UUID submissionId = randomUUID();
        final String status = "FAILED";
        final ProblemValue problemValue = ProblemValue.problemValue()
                .withId(randomUUID().toString())
                .withKey("key1")
                .withValue("value1")
                .build();
        final Problem problem = Problem.problem()
                .withCode("errCode1")
                .withValues(singletonList(problemValue))
                .build();
        final List<Problem> errors = singletonList(problem);

        final Stream<Object> eventStream = cpsSubmission.updateSubmissionStatus(submissionId, status, errors, null);

        final SubmissionStatusUpdated submissionStatusUpdated = (SubmissionStatusUpdated) eventStream.findFirst().get();
        assertThat(submissionStatusUpdated.getSubmissionId(), is(submissionStatusUpdated.getSubmissionId()));
        assertThat(submissionStatusUpdated.getSubmissionStatus().toString(), is(status));
        assertThat(submissionStatusUpdated.getErrors(), is(errors));
    }


}
