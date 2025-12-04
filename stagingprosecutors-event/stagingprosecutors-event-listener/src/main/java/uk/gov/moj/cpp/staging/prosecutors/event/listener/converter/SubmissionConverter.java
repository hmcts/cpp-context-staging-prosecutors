package uk.gov.moj.cpp.staging.prosecutors.event.listener.converter;

import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType.MATERIAL;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CourtApplicationSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCaseSubject;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;

import java.time.ZonedDateTime;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.MaterialSubmittedV3;

public class SubmissionConverter {

    public Submission convert(final  Envelope<MaterialSubmittedV3> envelope){
        final MaterialSubmittedV3 materialSubmitted = envelope.payload();
        final Submission submission = new Submission();
        ofNullable(materialSubmitted.getSubmissionId()).ifPresent(submission::setSubmissionId);

        ofNullable(materialSubmitted.getSubmissionStatus()).ifPresent(status->submission.setSubmissionStatus(status.toString()));
        final ProsecutionCaseSubject prosecutionCaseSubject =  materialSubmitted.getProsecutionCaseSubject();
        if (prosecutionCaseSubject != null) {
            ofNullable(prosecutionCaseSubject).ifPresent(caseSubject -> {
                ofNullable(caseSubject.getCaseUrn()).ifPresent(submission::setCaseUrn);
                ofNullable(caseSubject.getProsecutingAuthority()).ifPresent(submission::setOuCode);
            });
        }
        final CourtApplicationSubject courtApplicationSubject = materialSubmitted.getCourtApplicationSubject();
        if (courtApplicationSubject != null) {
            ofNullable(courtApplicationSubject).ifPresent(applicationSubject ->
                    ofNullable(applicationSubject.getCourtApplicationId()).ifPresent(submission::setApplicationId));
        }
        submission.setType(MATERIAL);
        submission.setReceivedAt(extractCreatedAt(envelope.metadata()));
        ofNullable(materialSubmitted.getIsCpsCase()).ifPresent(submission::setCpsCase);

        return submission;
    }

    private ZonedDateTime extractCreatedAt(final Metadata metadata) {
        return metadata.createdAt().orElseThrow(() -> new IllegalArgumentException("metadata createdAt is not present"));
    }

}
