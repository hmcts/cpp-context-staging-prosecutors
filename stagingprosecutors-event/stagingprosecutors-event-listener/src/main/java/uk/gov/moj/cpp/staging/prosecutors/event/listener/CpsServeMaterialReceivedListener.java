package uk.gov.moj.cpp.staging.prosecutors.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType.BCM;
import static uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType.COTR;
import static uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType.PET;
import static uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType.PTPH;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeBcmReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePtphReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsUpdateCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatusUpdated;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.SubmissionRepository;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class CpsServeMaterialReceivedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CpsServeMaterialReceivedListener.class);
    @Inject
    private SubmissionRepository submissionRepository;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("stagingprosecutors.event.cps-serve-pet-received")
    public void cpsServePetReceived(final Envelope<CpsServePetReceived> envelope) {
        LOGGER.info("stagingprosecutors.event.cps-serve-pet-received");
        final CpsServePetReceived cpsServePetReceived = envelope.payload();

        final Submission submission = new Submission(
                cpsServePetReceived.getSubmissionId(),
                cpsServePetReceived.getSubmissionStatus().toString(),
                cpsServePetReceived.getProsecutionCaseSubject().getUrn(),
                cpsServePetReceived.getProsecutionCaseSubject().getProsecutingAuthority(),
                null,
                null,
                PET,
                extractCreatedAt(envelope.metadata()),
                true, null);

        submissionRepository.save(submission);
    }

    @SuppressWarnings("squid:S3655")
    @Handles("stagingprosecutors.event.cps-serve-bcm-received")
    public void cpsServeBcmReceived(final Envelope<CpsServeBcmReceived> envelope) {
        LOGGER.info("stagingprosecutors.event.cps-serve-bcm-received");
        final CpsServeBcmReceived cpsServeBcmReceived = envelope.payload();

        String urn = null;
        String prosecutingAuthority = null;
        UUID submissionId = null;
        if (Objects.nonNull(cpsServeBcmReceived.getProsecutionCaseSubject())) {
            urn = cpsServeBcmReceived.getProsecutionCaseSubject().getUrn();
            prosecutingAuthority = cpsServeBcmReceived.getProsecutionCaseSubject().getProsecutingAuthority();
            submissionId = cpsServeBcmReceived.getSubmissionId();
        }

        final Submission submission = new Submission(
                submissionId,
                cpsServeBcmReceived.getSubmissionStatus().toString(),
                urn,
                prosecutingAuthority,
                null,
                null,
                BCM,
                extractCreatedAt(envelope.metadata()),
                true, null);

        submissionRepository.save(submission);
    }

    @Handles("stagingprosecutors.event.cps-serve-ptph-received")
    public void cpsServePtphReceived(final Envelope<CpsServePtphReceived> envelope) {
        LOGGER.info("stagingprosecutors.event.cps-serve-ptph-received");
        final CpsServePtphReceived cpsServePtphReceived = envelope.payload();

        final Submission submission = new Submission(
                cpsServePtphReceived.getSubmissionId(),
                cpsServePtphReceived.getSubmissionStatus().toString(),
                cpsServePtphReceived.getProsecutionCaseSubject().getUrn(),
                cpsServePtphReceived.getProsecutionCaseSubject().getProsecutingAuthority(),
                null,
                null,
                PTPH,
                extractCreatedAt(envelope.metadata()),
                true, null);

        submissionRepository.save(submission);
    }

    @Handles("stagingprosecutors.event.cps-serve-cotr-received")
    public void cpsServeCotrReceived(final Envelope<CpsServeCotrReceived> envelope) {
        LOGGER.info("stagingprosecutors.event.cps-serve-cotr-received");
        final CpsServeCotrReceived cpsServeCotrReceived = envelope.payload();

        final Submission submission = new Submission(
                cpsServeCotrReceived.getSubmissionId(),
                cpsServeCotrReceived.getSubmissionStatus().toString(),
                cpsServeCotrReceived.getProsecutionCaseSubject().getUrn(),
                cpsServeCotrReceived.getProsecutionCaseSubject().getProsecutingAuthority(),
                null,
                null,
                COTR,
                extractCreatedAt(envelope.metadata()),
                true, null);

        submissionRepository.save(submission);
    }

    @Handles("stagingprosecutors.event.cps-update-cotr-received")
    public void cpsUpdateCotrReceived(final Envelope<CpsUpdateCotrReceived> envelope) {
        LOGGER.info("stagingprosecutors.event.cps-update-cotr-received");
        final CpsUpdateCotrReceived cpsUpdateCotrReceived = envelope.payload();

        final Submission submission = new Submission(
                cpsUpdateCotrReceived.getSubmissionId(),
                cpsUpdateCotrReceived.getSubmissionStatus().toString(),
                cpsUpdateCotrReceived.getProsecutionCaseSubject().getUrn(),
                cpsUpdateCotrReceived.getProsecutionCaseSubject().getProsecutingAuthority(),
                null,
                null,
                COTR,
                extractCreatedAt(envelope.metadata()),
                true, null);

        submissionRepository.save(submission);
    }

    private ZonedDateTime extractCreatedAt(final Metadata metadata) {
        return metadata.createdAt().orElseThrow(() -> new IllegalArgumentException("metadata createdAt is not present"));
    }

    @Handles("stagingprosecutors.event.submission-status-updated")
    public void submissionStatusUpdated(final Envelope<SubmissionStatusUpdated> submissionStatusUpdatedEnvelope) {
        LOGGER.info("stagingprosecutors.event.submission-status-updated");
        final SubmissionStatusUpdated submissionStatusUpdated = submissionStatusUpdatedEnvelope.payload();
        LOGGER.info("Listener stagingprosecutors.event.submission-status-updated payload {}", submissionStatusUpdated);
        final Submission submission = submissionRepository.findBy(submissionStatusUpdated.getSubmissionId());

        submission.setSubmissionStatus(submissionStatusUpdated.getSubmissionStatus().toString());
        submission.setCompletedAt(extractCreatedAt(submissionStatusUpdatedEnvelope.metadata()));
        submission.setErrors(transformErrorsToJsonArray(submissionStatusUpdated.getErrors()));
        submission.setWarnings(transformErrorsToJsonArray(submissionStatusUpdated.getWarnings()));
    }

    private JsonArray transformErrorsToJsonArray(final Collection<Problem> errors) {
        if (errors == null) {
            return null;
        }

        final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        errors.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(arrayBuilder::add);

        return arrayBuilder.build();
    }
}
