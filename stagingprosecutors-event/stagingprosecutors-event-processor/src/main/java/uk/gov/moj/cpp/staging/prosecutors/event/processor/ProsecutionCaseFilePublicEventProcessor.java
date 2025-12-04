package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.RejectMaterial.rejectMaterial;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.RejectMaterial;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.UpdateSubmissionStatus;
import uk.gov.moj.cps.stagingprosecutors.domain.event.CpsServeMaterialStatusUpdatedEvent;
import uk.gov.moj.cps.stagingprosecutors.domain.event.PublicMaterialPendingWithWarnings;
import uk.gov.moj.cps.stagingprosecutors.domain.event.PublicMaterialRejected;
import uk.gov.moj.cps.stagingprosecutors.domain.event.PublicMaterialRejectedV2;
import uk.gov.moj.cps.stagingprosecutors.domain.event.PublicMaterialRejectedWithWarnings;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionCaseFilePublicEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseFilePublicEventProcessor.class);
    private static final String SUBMISSION_ID_NOT_FOUND = "Submission ID not found. Material rejected event ignored";
    private static final String STAGING_PROSECUTORS_COMMAND_REJECT_MATERIAL = "stagingprosecutors.command.reject-material";
    private static final String STAGING_PROSECUTORS_COMMAND_MATERIAL_PENDING_WITH_WARNINGS = "stagingprosecutors.command.material-pending-with-warnings";
    private static final String SUBMISSION_ID = "submissionId";

    @Inject
    private Sender sender;

    @Handles("public.prosecutioncasefile.material-rejected")
    public void caseMaterialRejected(final Envelope<PublicMaterialRejected> materialRejectedEnvelope) {

        final Optional<UUID> submissionId = ofNullable(materialRejectedEnvelope.metadata().asJsonObject().getString(SUBMISSION_ID, null))
                .map(UUID::fromString);

        if (!submissionId.isPresent()) {
            LOGGER.info(SUBMISSION_ID_NOT_FOUND);
            return;
        }

        final Metadata rejectMaterialCommandMetadata = metadataFrom(materialRejectedEnvelope.metadata())
                .withName(STAGING_PROSECUTORS_COMMAND_REJECT_MATERIAL)
                .build();

        final RejectMaterial rejectMaterialCommandPayload = rejectMaterial()
                .withErrors(materialRejectedEnvelope.payload().getErrors())
                .withSubmissionId(submissionId.get())
                .build();

        final Envelope<RejectMaterial> envelope = envelopeFrom(rejectMaterialCommandMetadata, rejectMaterialCommandPayload);

        sender.send(envelope);
    }

    @Handles("public.prosecutioncasefile.material-rejected-v2")
    public void caseMaterialRejectedV2(final Envelope<PublicMaterialRejectedV2> materialRejectedEnvelope) {

        final Optional<UUID> submissionId = ofNullable(materialRejectedEnvelope.metadata().asJsonObject().getString(SUBMISSION_ID, null))
                .map(UUID::fromString);

        if (!submissionId.isPresent()) {
            LOGGER.info(SUBMISSION_ID_NOT_FOUND);
            return;
        }

        final RejectMaterial rejectMaterialCommandPayload = rejectMaterial()
                .withErrors(materialRejectedEnvelope.payload().getErrors())
                .withSubmissionId(submissionId.get())
                .build();

        final Metadata rejectMaterialCommandMetadata = metadataFrom(materialRejectedEnvelope.metadata())
                .withName(STAGING_PROSECUTORS_COMMAND_REJECT_MATERIAL)
                .build();

        final Envelope<RejectMaterial> envelope = envelopeFrom(rejectMaterialCommandMetadata, rejectMaterialCommandPayload);

        sender.send(envelope);
    }

    @Handles("public.prosecutioncasefile.material-rejected-with-warnings")
    public void caseMaterialRejectedWithWarnings(final Envelope<PublicMaterialRejectedWithWarnings> materialRejectedEnvelope) {

        final Optional<UUID> submissionId = ofNullable(materialRejectedEnvelope.metadata().asJsonObject().getString(SUBMISSION_ID, null))
                .map(UUID::fromString);

        if (!submissionId.isPresent()) {
            LOGGER.info(SUBMISSION_ID_NOT_FOUND);
            return;
        }

        final RejectMaterial rejectMaterialCommandPayload = rejectMaterial()
                .withErrors(materialRejectedEnvelope.payload().getErrors())
                .withWarnings(materialRejectedEnvelope.payload().getWarnings())
                .withSubmissionId(submissionId.get())
                .build();

        final Metadata rejectMaterialCommandMetadata = metadataFrom(materialRejectedEnvelope.metadata())
                .withName(STAGING_PROSECUTORS_COMMAND_REJECT_MATERIAL)
                .build();

        final Envelope<RejectMaterial> envelope = envelopeFrom(rejectMaterialCommandMetadata, rejectMaterialCommandPayload);

        sender.send(envelope);
    }

    @Handles("public.prosecutioncasefile.material-pending-with-warnings")
    public void materialSubmissionPendingWithWarnings(final Envelope<PublicMaterialPendingWithWarnings> materialRejectedEnvelope) {

        final Optional<UUID> submissionId = ofNullable(materialRejectedEnvelope.metadata().asJsonObject().getString(SUBMISSION_ID, null))
                .map(UUID::fromString);

        if (!submissionId.isPresent()) {
            LOGGER.info(SUBMISSION_ID_NOT_FOUND);
            return;
        }

        final RejectMaterial rejectMaterialCommandPayload = rejectMaterial()
                .withWarnings(materialRejectedEnvelope.payload().getWarnings())
                .withSubmissionId(submissionId.get())
                .build();

        final Metadata rejectMaterialCommandMetadata = metadataFrom(materialRejectedEnvelope.metadata())
                .withName(STAGING_PROSECUTORS_COMMAND_MATERIAL_PENDING_WITH_WARNINGS)
                .build();

        final Envelope<RejectMaterial> envelope = envelopeFrom(rejectMaterialCommandMetadata, rejectMaterialCommandPayload);

        sender.send(envelope);
    }

    @Handles("public.prosecutioncasefile.cps-serve-material-status-updated")
    public void cpsServeMaterialUpdateStatus(final Envelope<CpsServeMaterialStatusUpdatedEvent> cpsServeMaterialStatusUpdatedEventEnvelope) {
        LOGGER.info("public.prosecutioncasefile.cps-serve-material-status-updated event...");

        final CpsServeMaterialStatusUpdatedEvent cpsServeMaterialStatusUpdatedEvent = cpsServeMaterialStatusUpdatedEventEnvelope.payload();
        final UUID submissionId = cpsServeMaterialStatusUpdatedEvent.getSubmissionId();

        if (submissionId == null) {
            LOGGER.info("Submission ID not found. public.prosecutioncasefile.cps-serve-material-status-updated event ignored");
            return;
        }

        final Metadata submissionStatusCommandMetadata = metadataFrom(cpsServeMaterialStatusUpdatedEventEnvelope.metadata())
                .withName("stagingprosecutors.command.update-submission-status")
                .build();
        final uk.gov.moj.cps.stagingprosecutors.domain.event.SubmissionStatus submissionStatus = cpsServeMaterialStatusUpdatedEvent.getSubmissionStatus();

        UpdateSubmissionStatus.Builder builder = UpdateSubmissionStatus.updateSubmissionStatus()
                .withSubmissionId(submissionId);
        if (submissionStatus.equals(uk.gov.moj.cps.stagingprosecutors.domain.event.SubmissionStatus.EXPIRED)) {
            final uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem problem = uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem.problem()
                    .withCode("CASE_URN_NOT_FOUND")
                    .withValues(of(problemValue().withKey("Expired").withValue("Submission has been in a pending state for 28 days").build()))
                    .build();
            final List<uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem> errors = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(cpsServeMaterialStatusUpdatedEvent.getErrors())) {
                errors.addAll(cpsServeMaterialStatusUpdatedEvent.getErrors());
            }
            errors.add(problem);
            builder = builder.withSubmissionStatus(SubmissionStatus.FAILED)
                    .withErrors(errors);
        } else {
            builder = builder
                    .withWarnings(cpsServeMaterialStatusUpdatedEvent.getWarnings())
                    .withErrors(cpsServeMaterialStatusUpdatedEvent.getErrors())
                    .withSubmissionStatus(SubmissionStatus.valueOf(submissionStatus.toString()));
        }

        final Envelope<UpdateSubmissionStatus> envelope = envelopeFrom(submissionStatusCommandMetadata, builder.build());
        LOGGER.info("Raise stagingprosecutors.command.update-submission-status with submission id {}",submissionId );

        sender.send(envelope);
    }
}
