package uk.gov.moj.cpp.staging.prosecutors.domain;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeBcmReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePtphReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsUpdateCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatusUpdated;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
@SuppressWarnings({"squid:S1450"})
public class CpsSubmission implements Aggregate {

    private SubmissionStatus submissionStatus;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(CpsServePetReceived.class).apply(e -> submissionStatus = PENDING),
                when(CpsServeBcmReceived.class).apply(e -> submissionStatus = PENDING),
                when(CpsServePtphReceived.class).apply(e -> submissionStatus = PENDING),
                when(CpsServeCotrReceived.class).apply(e -> submissionStatus = PENDING),
                when(CpsUpdateCotrReceived.class).apply(e -> submissionStatus = PENDING),
                when(SubmissionStatusUpdated.class).apply(e -> submissionStatus = e.getSubmissionStatus()),

                otherwiseDoNothing()
        );
    }

    public Stream<Object> receivePetSubmission(final CpsServePetReceived cpsServePetReceived) {

        return apply(Stream.of(CpsServePetReceived.cpsServePetReceived()
                .withValuesFrom(cpsServePetReceived)
                .withSubmissionStatus(PENDING)
                .build()));
    }

    public Stream<Object> receiveCotrSubmission(final CpsServeCotrReceived cpsServeCotrReceived) {

        return apply(Stream.of(CpsServeCotrReceived.cpsServeCotrReceived()
                .withValuesFrom(cpsServeCotrReceived)
                .withSubmissionStatus(PENDING)
                .build()));
    }

    public Stream<Object> receivePtphSubmission(final CpsServePtphReceived cpsServePtphReceived) {

        return apply(Stream.of(CpsServePtphReceived.cpsServePtphReceived()
                .withValuesFrom(cpsServePtphReceived)
                .withSubmissionStatus(PENDING)
                .build()));
    }

    public Stream<Object> receiveBcmSubmission(final CpsServeBcmReceived cpsServeBcmReceived) {

        return apply(Stream.of(CpsServeBcmReceived.cpsServeBcmReceived()
                .withValuesFrom(cpsServeBcmReceived)
                .withSubmissionStatus(PENDING)
                .build()));
    }

    public Stream<Object> receiveUpdateCotrSubmission(final CpsUpdateCotrReceived cpsUpdateCotrReceived) {

        return apply(Stream.of(CpsUpdateCotrReceived.cpsUpdateCotrReceived()
                .withValuesFrom(cpsUpdateCotrReceived)
                .withSubmissionStatus(PENDING)
                .build()));
    }

    public Stream<Object> updateSubmissionStatus(final UUID submissionId, final String status, final List<Problem> errors, final List<Problem> warnings) {
        return apply(Stream.of(SubmissionStatusUpdated.submissionStatusUpdated()
                .withSubmissionId(submissionId)
                .withSubmissionStatus(SubmissionStatus.valueOf(status))
                .withErrors(errors)
                .withWarnings(warnings)
                .build()));
    }

}
