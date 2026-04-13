package uk.gov.moj.cpp.staging.prosecutors.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.SUCCESS;
import static uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType.MATERIAL;
import static uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType.PROSECUTION;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.event.listener.converter.SubmissionConverter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionPendingWithWarnings;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionSuccessfulWithWarnings;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.SubmissionRepository;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsMaterialSubmitted;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.MaterialSubmittedV3;

@ServiceComponent(EVENT_LISTENER)
public class SubmissionEventListener {

    @Inject
    private SubmissionRepository submissionRepository;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    SubmissionConverter submissionConverter;

    @Handles("stagingprosecutors.event.prosecution-received")
    public void submissionRequestReceived(final Envelope<ProsecutionReceived> envelope) {
        final ProsecutionReceived prosecutionReceived = envelope.payload();

        final Submission submission = new Submission(
                prosecutionReceived.getSubmissionId(),
                prosecutionReceived.getSubmissionStatus().toString(),
                prosecutionReceived.getProsecutionSubmissionDetails().getUrn(),
                prosecutionReceived.getProsecutionSubmissionDetails().getProsecutingAuthority(),
                null,
                null,
                PROSECUTION,
                extractCreatedAt(envelope.metadata()),
                false,
                null);

        submissionRepository.save(submission);
    }

    @Handles("stagingprosecutors.event.sjp-prosecution-received")
    public void sjpSubmissionRequestReceived(final Envelope<SjpProsecutionReceived> envelope) {
        final SjpProsecutionReceived sjpProsecutionReceived = envelope.payload();

        final Submission submission = new Submission(
                sjpProsecutionReceived.getSubmissionId(),
                sjpProsecutionReceived.getSubmissionStatus().toString(),
                sjpProsecutionReceived.getProsecutionSubmissionDetails().getUrn(),
                sjpProsecutionReceived.getProsecutionSubmissionDetails().getProsecutingAuthority(),
                null,
                null,
                PROSECUTION,
                extractCreatedAt(envelope.metadata()),
                false,
                null
        );

        submissionRepository.save(submission);
    }

    @Handles("stagingprosecutors.event.submission-successful")
    public void submissionSuccessfulReceived(final Envelope<SubmissionSuccessful> envelope) {
        final SubmissionSuccessful submissionSuccessful = envelope.payload();

        final Submission submission = submissionRepository.findBy(submissionSuccessful.getSubmissionId());

        submission.setSubmissionStatus(SubmissionStatus.SUCCESS.toString());
        submission.setCompletedAt(extractCreatedAt(envelope.metadata()));
    }

    @Handles("stagingprosecutors.event.submission-successful-with-warnings")
    public void submissionSuccessfulReceivedWithWarnings(final Envelope<SubmissionSuccessfulWithWarnings> envelope) {
        final SubmissionSuccessfulWithWarnings submissionSuccessfulWithWarnings = envelope.payload();

        final Submission submission = submissionRepository.findBy(submissionSuccessfulWithWarnings.getSubmissionId());
        submission.setSubmissionStatus(SubmissionStatus.SUCCESS_WITH_WARNINGS.toString());

        final List<Problem> warnings = submissionSuccessfulWithWarnings.getWarnings();
        final List<DefendantProblem> defendantProblemList = submissionSuccessfulWithWarnings.getDefendantWarnings();
        final JsonArray submissionWarnings = transformErrorsOrWarningsToJsonArray(warnings);
        final JsonArray defendantWarnings = transformDefendantProblemsToJsonArray(defendantProblemList);

        submission.setWarnings(submissionWarnings);
        submission.setCompletedAt(extractCreatedAt(envelope.metadata()));
        submission.setDefendantWarnings(defendantWarnings);
    }

    @Handles("stagingprosecutors.event.submission-rejected")
    public void submissionRejected(final Envelope<SubmissionRejected> envelope) {
        final SubmissionRejected submissionRejected = envelope.payload();


        final Submission submission = submissionRepository.findBy(submissionRejected.getSubmissionId());

        submission.setSubmissionStatus(SubmissionStatus.REJECTED.toString());
        submission.setCompletedAt(extractCreatedAt(envelope.metadata()));
        submission.setErrors(transformErrorsOrWarningsToJsonArray(submissionRejected.getErrors()));
        submission.setCaseErrors(transformErrorsOrWarningsToJsonArray(submissionRejected.getCaseErrors()));
        submission.setDefendantErrors(transformDefendantProblemsToJsonArray(submissionRejected.getDefendantErrors()));

    }

    @Handles("stagingprosecutors.event.submission-pending-with-warnings")
    public void materialSubmissionPendingWithWarnings(final Envelope<SubmissionPendingWithWarnings> envelope) {
        final SubmissionPendingWithWarnings submissionPendingWithWarnings = envelope.payload();
        final Submission submission = submissionRepository.findBy(submissionPendingWithWarnings.getSubmissionId());
        submission.setSubmissionStatus(SubmissionStatus.PENDING_WITH_WARNINGS.toString());
        submission.setWarnings(transformErrorsOrWarningsToJsonArray(submissionPendingWithWarnings.getWarnings()));
    }

    @Handles("stagingprosecutors.event.material-submitted")
    public void materialSubmitted(final Envelope<MaterialSubmitted> envelope) {
        final MaterialSubmitted materialSubmitted = envelope.payload();

        final Submission submission = new Submission(
                materialSubmitted.getSubmissionId(),
                materialSubmitted.getSubmissionStatus().toString(),
                materialSubmitted.getCaseUrn(),
                materialSubmitted.getProsecutingAuthority(),
                null,
                null,
                MATERIAL,
                extractCreatedAt(envelope.metadata()),
                materialSubmitted.getIsCpsCase(),
                null);

        submissionRepository.save(submission);
    }

    @Handles("stagingprosecutors.event.material-submitted-v3")
    public void materialSubmittedV3(final Envelope<MaterialSubmittedV3> envelope) {
        final Submission submission = submissionConverter.convert(envelope);
        submissionRepository.save(submission);
    }

    @Handles("stagingprosecutors.event.cps-material-submitted")
    public void cpsMaterialSubmitted(final Envelope<CpsMaterialSubmitted> envelope) {
        final CpsMaterialSubmitted materialSubmitted = envelope.payload();

        final Submission submission = new Submission(
                materialSubmitted.getSubmissionId(),
                materialSubmitted.getSubmissionStatus().toString(),
                materialSubmitted.getUrn(),
                "CPS",
                null,
                null,
                MATERIAL,
                extractCreatedAt(envelope.metadata()),
                true,
                null);

        submissionRepository.save(submission);
    }



    @Handles("stagingprosecutors.event.material-submission-successful")
    public void materialSubmissionSuccessfulReceived(final Envelope<MaterialSubmissionSuccessful> envelope) {
        final MaterialSubmissionSuccessful materialSubmissionSuccessful = envelope.payload();
        final Submission submission = submissionRepository.findBy(materialSubmissionSuccessful.getSubmissionId());

        submission.setCompletedAt(extractCreatedAt(envelope.metadata()));
        submission.setSubmissionStatus(SUCCESS.toString());
    }

    @Handles("stagingprosecutors.event.material-submission-rejected")
    public void materialSubmissionRejected(final Envelope<MaterialSubmissionRejected> envelope) {
        final MaterialSubmissionRejected submissionRejected = envelope.payload();
        submissionRejected(submissionRejected.getSubmissionId(), submissionRejected.getErrors(), submissionRejected.getWarnings(), extractCreatedAt(envelope.metadata()));
    }

    private void submissionRejected(final UUID submissionId, final List<Problem> errors, final List<Problem> warnings, final ZonedDateTime timestamp) {
        final JsonArray submissionErrors = transformErrorsOrWarningsToJsonArray(errors);
        final JsonArray submissionWarnings = transformErrorsOrWarningsToJsonArray(warnings);
        final Submission submission = submissionRepository.findBy(submissionId);

        submission.setSubmissionStatus(SubmissionStatus.REJECTED.toString());
        submission.setCompletedAt(timestamp);
        submission.setErrors(submissionErrors);
        submission.setWarnings(submissionWarnings);
    }

    private ZonedDateTime extractCreatedAt(final Metadata metadata) {
        return metadata.createdAt().orElseThrow(() -> new IllegalArgumentException("metadata createdAt is not present"));
    }

    private JsonArray transformErrorsOrWarningsToJsonArray(final Collection<Problem> errorsOrWarnings) {
        if (errorsOrWarnings == null) {
            return null;
        }


        final JsonArrayBuilder arrayBuilder = createArrayBuilder();

        errorsOrWarnings.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(arrayBuilder::add);

        return arrayBuilder.build();
    }

    private JsonArray transformDefendantProblemsToJsonArray(final Collection<DefendantProblem> errors) {

        if (errors == null) {
            return null;
        }

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();

        errors.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(arrayBuilder::add);

        return arrayBuilder.build();
    }
}
