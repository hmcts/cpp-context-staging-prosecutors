package uk.gov.moj.cpp.staging.prosecutors.domain;

import static java.util.stream.Stream.builder;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionReceived.prosecutionReceived;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionReceived.sjpProsecutionReceived;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionNotMarkedAsPending.submissionNotMarkedAsPending;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionRejected.submissionRejected;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.REJECTED;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.SUCCESS;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.SUCCESS_WITH_WARNINGS;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionSuccessful.submissionSuccessful;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionSuccessfulWithWarnings.submissionSuccessfulWithWarnings;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionSuccessfulWithWarnings;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ProsecutionSubmission implements Aggregate {

    private SubmissionStatus submissionStatus;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(ProsecutionReceived.class).apply(e ->
                        submissionStatus = PENDING
                ),
                when(SjpProsecutionReceived.class).apply(e ->
                        submissionStatus = PENDING
                ),
                when(SubmissionSuccessful.class).apply(e ->
                        submissionStatus = SUCCESS
                ),
                when(SubmissionSuccessfulWithWarnings.class).apply(e ->
                        submissionStatus = SUCCESS_WITH_WARNINGS
                ),
                when(SubmissionRejected.class).apply(e ->
                        submissionStatus = REJECTED
                ),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> receiveSubmission(final UUID submissionId,
                                            final ProsecutionSubmissionDetails prosecutionSubmissionDetails,
                                            final List<Defendant> defendants) {

        return apply(Stream.of(prosecutionReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .withProsecutionSubmissionDetails(prosecutionSubmissionDetails)
                .withDefendants(defendants).build()));
    }

    public Stream<Object> receiveSjpSubmission(final UUID submissionId,
                                               final SjpProsecutionSubmissionDetails prosecutionSubmissionDetails,
                                               final SjpDefendant defendant) {

        return apply(Stream.of(sjpProsecutionReceived()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(PENDING)
                .withProsecutionSubmissionDetails(prosecutionSubmissionDetails)
                .withDefendant(defendant).build()));
    }

    public Stream<Object> receiveSubmissionSuccessful(final UUID submissionId) {
        return handleSubmissionReceipt(submissionId,
                () -> submissionSuccessful()
                        .withSubmissionId(submissionId)
                        .build());
    }

    public Stream<Object> receiveSubmissionSuccessfulWithWarnings(final UUID submissionId, final List<Problem> warnings, final List<DefendantProblem> defendantWarnings) {
        return handleSubmissionReceipt(submissionId,
                () -> submissionSuccessfulWithWarnings()
                        .withSubmissionId(submissionId)
                        .withWarnings(warnings)
                        .withDefendantWarnings(defendantWarnings)
                        .build());
    }

    private Stream<Object> handleSubmissionReceipt(final UUID submissionId, final Supplier<Object> submissionStatusEventSupplier) {
        final Stream.Builder<Object> builder = builder();

        if (submissionStatus == PENDING) {
            builder.add(submissionStatusEventSupplier.get());
        } else {
            builder.add(submissionNotMarkedAsPending()
                    .withSubmissionId(submissionId)
                    .build());
        }

        return apply(builder.build());
    }

    public Stream<Object> receiveSubmissionRejection(final UUID submissionId, final List<Problem> errors, final List<Problem> caseErrors, final List<DefendantProblem> defendantErrors) {

        final Stream.Builder<Object> builder = builder();

        if (submissionStatus == PENDING) {
            builder.add(submissionRejected()
                    .withSubmissionId(submissionId)
                    .withErrors(errors)
                    .withCaseErrors(caseErrors)
                    .withDefendantErrors(defendantErrors)
                    .build());
        } else {
            builder.add(submissionNotMarkedAsPending()
                    .withSubmissionId(submissionId)
                    .build());
        }

        return apply(builder.build());
    }

}
