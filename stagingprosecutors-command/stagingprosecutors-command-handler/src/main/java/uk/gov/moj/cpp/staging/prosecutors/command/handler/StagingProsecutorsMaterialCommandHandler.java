package uk.gov.moj.cpp.staging.prosecutors.command.handler;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.domain.MaterialSubmission;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialPendingWithWarnings;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveMaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.RejectSubmission;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmitCpsMaterialCommand;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmitMaterialCommand;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmitMaterialCommandV3;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmitMaterialV3;

@ServiceComponent(COMMAND_HANDLER)
public class StagingProsecutorsMaterialCommandHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("stagingprosecutors.command.submit-material")
    public void handleSubmitMaterial(final Envelope<SubmitMaterialCommand> command) throws EventStreamException {
        final SubmitMaterialCommand payload = command.payload();

        applyToAggregate(payload.getSubmissionId(), command, materialSubmission -> materialSubmission.submitMaterial(
                payload.getSubmissionId(),
                payload.getMaterialId(),
                payload.getCaseUrn(),
                payload.getProsecutingAuthority(),
                payload.getMaterialType(),
                ofNullable(payload.getDefendantId()),
                ofNullable(payload.getIsCpsCase()))
        );
    }

    @Handles("stagingprosecutors.command.submit-material-v3")
    public void handleSubmitMaterialV3(final Envelope<SubmitMaterialCommandV3> command) throws EventStreamException {

        final SubmitMaterialCommandV3 submitMaterialPayload = command.payload();

        final UUID submissionId = submitMaterialPayload.getSubmissionId();

        final EventStream eventStream = eventSource.getStreamById(submissionId);

        final MaterialSubmission materialSubmission = aggregateService.get(eventStream, MaterialSubmission.class);

        final Stream<Object> events = materialSubmission.submitMaterialV3(submissionId, createSubmitMaterial(submitMaterialPayload));

        final JsonEnvelope jsonEnvelope = envelopeFrom(command.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));

    }

    @Handles("stagingprosecutors.command.submit-cps-material")
    public void handleSubmitCpsMaterial(final Envelope<SubmitCpsMaterialCommand> command) throws EventStreamException {
        final SubmitCpsMaterialCommand payload = command.payload();

        applyToAggregate(payload.getSubmissionId(), command, materialSubmission -> materialSubmission.submitCpsMaterial(
                payload.getSubmissionId(),
                payload.getTransactionID(),
                payload.getUrn(),
                payload.getCompassCaseId(),
                ofNullable(payload.getResponseEmail()),
                payload.getDefendants(),
                payload.getDocuments()
                )
        );
    }

    @Handles("stagingprosecutors.command.receive-material-submission-successful")
    public void handleReceiveMaterialSubmissionSuccessful(final Envelope<ReceiveMaterialSubmissionSuccessful> command) throws EventStreamException {
        final UUID submissionId = command.payload().getSubmissionId();
        applyToAggregate(submissionId, command, materialSubmission -> materialSubmission.receiveMaterialSubmissionSuccessful(submissionId));
    }

    @Handles("stagingprosecutors.command.reject-material")
    public void handleReceiveMaterialSubmissionRejected(final Envelope<RejectSubmission> command) throws EventStreamException {
        final RejectSubmission payload = command.payload();
        applyToAggregate(
                payload.getSubmissionId(),
                command,
                materialSubmission -> materialSubmission.rejectMaterial(payload.getErrors(), payload.getWarnings()));
    }

    @Handles("stagingprosecutors.command.material-pending-with-warnings")
    public void handleMaterialSubmissionPendingWithWarnings(final Envelope<MaterialPendingWithWarnings> command) throws EventStreamException {
        final MaterialPendingWithWarnings payload = command.payload();
        applyToAggregate(
                payload.getSubmissionId(),
                command,
                materialSubmission -> materialSubmission.materialPendingWithWarning(payload.getWarnings()));
    }


    private void applyToAggregate(final UUID submissionId, final Envelope command, Function<MaterialSubmission, Stream<Object>> aggregateFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(submissionId);
        final MaterialSubmission materialSubmission = aggregateService.get(eventStream, MaterialSubmission.class);

        final Stream<Object> events = aggregateFunction.apply(materialSubmission);

        final JsonEnvelope jsonEnvelope = envelopeFrom(command.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    private SubmitMaterialV3 createSubmitMaterial(final SubmitMaterialCommandV3 submitMaterialCommand){
        return new SubmitMaterialV3.Builder()
                .withMaterial(submitMaterialCommand.getMaterial())
                .withMaterialContentType(submitMaterialCommand.getMaterialContentType())
                .withMaterialName(submitMaterialCommand.getMaterialName())
                .withMaterialType(submitMaterialCommand.getMaterialType())
                .withSectionOrderSequence(submitMaterialCommand.getSectionOrderSequence())
                .withFileName(submitMaterialCommand.getFileName())
                .withCaseSubFolderName(submitMaterialCommand.getCaseSubFolderName())
                .withCourtApplicationSubject(submitMaterialCommand.getCourtApplicationSubject())
                .withProsecutionCaseSubject(submitMaterialCommand.getProsecutionCaseSubject())
                .withExhibit(submitMaterialCommand.getExhibit())
                .withWitnessStatement(submitMaterialCommand.getWitnessStatement())
                .withTag(submitMaterialCommand.getTag())
                .build();
    }
}
