package uk.gov.moj.cpp.staging.prosecutors.domain;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProblemValue;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionNotMarkedAsPending;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionSuccessfulWithWarnings;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionSubmissionTest {

    public static final String KEY_1 = "key1";
    public static final String VALUE_1 = "value1";
    public static final String ERR_CODE_1 = "errCode1";
    @InjectMocks
    private ProsecutionSubmission prosecutionSubmission;

    @Test
    public void shouldRaiseProsecutionReceivedEvent() {

        final UUID submissionId = randomUUID();
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.prosecutionSubmissionDetails()
                .withUrn("urn")
                .build();
        final List<Defendant> defendants = singletonList(Defendant.defendant().withDefendantDetails(DefendantDetails.defendantDetails().withAsn("asn").build()).build());

        final Stream<Object> eventStream = prosecutionSubmission.receiveSubmission(submissionId, prosecutionSubmissionDetails, defendants);

        final ProsecutionReceived prosecutionReceived = (ProsecutionReceived) eventStream.findFirst().get();
        assertThat(prosecutionReceived.getSubmissionId(), is(submissionId));
        assertThat(prosecutionReceived.getSubmissionStatus(), is(PENDING));
        assertThat(prosecutionReceived.getProsecutionSubmissionDetails(), is(prosecutionSubmissionDetails));
    }

    @Test
    public void shouldRaiseSjpProsecutionReceivedEvent() {

        final UUID submissionId = randomUUID();
        final SjpProsecutionSubmissionDetails sjpProsecutionSubmissionDetails = SjpProsecutionSubmissionDetails.sjpProsecutionSubmissionDetails()
                .withUrn("urn")
                .build();
        final SjpDefendant defendant = SjpDefendant.sjpDefendant().withAsn("asn").build();

        final Stream<Object> eventStream = prosecutionSubmission.receiveSjpSubmission(submissionId, sjpProsecutionSubmissionDetails, defendant);

        final SjpProsecutionReceived sjpProsecutionReceived = (SjpProsecutionReceived) eventStream.findFirst().get();
        assertThat(sjpProsecutionReceived.getSubmissionId(), is(submissionId));
        assertThat(sjpProsecutionReceived.getSubmissionStatus(), is(PENDING));
        assertThat(sjpProsecutionReceived.getDefendant(), is(defendant));
        assertThat(sjpProsecutionReceived.getProsecutionSubmissionDetails(), is(sjpProsecutionSubmissionDetails));
    }

    @Test
    public void shouldRaiseSubmissionSuccessfulWithWarningsEventWhenSubmissionStatusIsPending() {

        final UUID submissionId = randomUUID();
        final Problem problem = getProblem();
        final List<Problem> warnings = singletonList(problem);
        final List<DefendantProblem> defendantProblems = singletonList(DefendantProblem.defendantProblem().withProblems(singletonList(problem)).build());

        prosecutionSubmission.apply(ProsecutionReceived.prosecutionReceived().build());
        final Stream<Object> eventStream = prosecutionSubmission.receiveSubmissionSuccessfulWithWarnings(submissionId, warnings, defendantProblems);

        final SubmissionSuccessfulWithWarnings submissionSuccessfulWithWarnings = (SubmissionSuccessfulWithWarnings) eventStream.findFirst().get();
        assertThat(submissionSuccessfulWithWarnings.getSubmissionId(), is(submissionId));
        assertThat(submissionSuccessfulWithWarnings.getWarnings(), is(warnings));
        assertThat(submissionSuccessfulWithWarnings.getDefendantWarnings(), is(defendantProblems));
    }

    @Test
    public void shouldRaiseSubmissionNotMarkedAsPendingEventWhenSubmissionStatusIsNotPending() {

        final UUID submissionId = randomUUID();
        final Problem problem = getProblem();
        final List<Problem> warnings = singletonList(problem);
        final List<DefendantProblem> defendantProblems = singletonList(DefendantProblem.defendantProblem().withProblems(singletonList(problem)).build());

        final Stream<Object> eventStream = prosecutionSubmission.receiveSubmissionSuccessfulWithWarnings(submissionId, warnings, defendantProblems);

        final SubmissionNotMarkedAsPending submissionNotMarkedAsPending = (SubmissionNotMarkedAsPending) eventStream.findFirst().get();
        assertThat(submissionNotMarkedAsPending.getSubmissionId(), is(submissionId));
    }

    @Test
    public void shouldRaiseSubmissionRejectedEventWhenSubmissionStatusIsPending() {

        final UUID submissionId = randomUUID();
        final Problem problem = getProblem();
        final List<Problem> errors = singletonList(problem);

        prosecutionSubmission.apply(ProsecutionReceived.prosecutionReceived().build());
        final Stream<Object> eventStream = prosecutionSubmission.receiveSubmissionRejection(submissionId, errors, null, null);

        final SubmissionRejected submissionNotMarkedAsPending = (SubmissionRejected) eventStream.findFirst().get();
        assertThat(submissionNotMarkedAsPending.getSubmissionId(), is(submissionId));
        assertThat(submissionNotMarkedAsPending.getErrors(), is(errors));
    }

    @Test
    public void shouldRaiseSubmissionNotMarkedAsPendingWhenSubmissionStatusIsNotPending() {

        final UUID submissionId = randomUUID();
        final Problem problem = getProblem();
        final List<Problem> errors = singletonList(problem);

        final Stream<Object> eventStream = prosecutionSubmission.receiveSubmissionRejection(submissionId, errors, null, null);

        final SubmissionNotMarkedAsPending notMarkedAsPending = (SubmissionNotMarkedAsPending) eventStream.findFirst().get();
        assertThat(notMarkedAsPending.getSubmissionId(), is(submissionId));
    }


    private static Problem getProblem() {
        final ProblemValue problemValue = ProblemValue.problemValue()
                .withId(randomUUID().toString())
                .withKey(KEY_1)
                .withValue(VALUE_1)
                .build();
        return Problem.problem()
                .withCode(ERR_CODE_1)
                .withValues(singletonList(problemValue))
                .build();
    }


}
