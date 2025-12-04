package uk.gov.moj.cpp.staging.prosecutors.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.RecordUnbundleDocumentResults;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Material;
import uk.gov.moj.cpp.staging.prosecutors.domain.RecordDocumentUnbundleResult;
import uk.gov.moj.cpp.staging.prosecutors.domain.UnbundleSubmission;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class StagingProsecutorsUnbundleCommandHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingProsecutorsUnbundleCommandHandler.class.getName());

    @Handles("stagingprosecutors.command.record-document-unbundle-result")
    public void handleUnbundleResult(final Envelope<RecordDocumentUnbundleResult> command) throws EventStreamException {
        LOGGER.debug("stagingprosecutors.command.record-document-unbundle-result {}", command);
        final RecordDocumentUnbundleResult payload = command.payload();

        final UUID caseId = payload.getCaseId();
        final String prosecutorDefendantId = payload.getProsecutorDefendantId();
        final Optional<ZonedDateTime> receivedDateTime = payload.getReceivedDateTime();
        final Optional<String> prosecutingAuthority = payload.getProsecutingAuthority();
        final Optional<String> errorMessage = payload.getErrorMessage();
        final Optional<Material> material = payload.getMaterial();

        applyToAggregate(caseId, command, u -> u.submitUnbundleResultRecord(caseId, prosecutorDefendantId,
                prosecutingAuthority,
                receivedDateTime,
                material,
                errorMessage));
    }

    @Handles("stagingprosecutors.command.record-unbundled-document-results")
    public void handleUnbundleDocumentResults(final Envelope<RecordUnbundleDocumentResults> command) throws EventStreamException{

        final RecordUnbundleDocumentResults recordUnbundleDocumentResults = command.payload();
        final UUID caseId = recordUnbundleDocumentResults.getCaseId();
        final String prosecutorDefendantId = recordUnbundleDocumentResults.getProsecutorDefendantId();
        final ZonedDateTime  receivedDateTime = recordUnbundleDocumentResults.getReceivedDateTime();
        final  String prosecutingAuthority = recordUnbundleDocumentResults.getProsecutingAuthority();
        final String errorMessage = recordUnbundleDocumentResults.getErrorMessage();
        final List<Material> materials = recordUnbundleDocumentResults.getMaterials();

        applyToAggregate(caseId, command, u -> u.submitUnbundleDocumentResults(caseId, prosecutorDefendantId,
                prosecutingAuthority,
                receivedDateTime,
                materials,
                errorMessage));
    }

    private void applyToAggregate(final UUID caseId, final Envelope command, Function<UnbundleSubmission, Stream<Object>> aggregateFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(caseId);
        final UnbundleSubmission unbundleSubmission = aggregateService.get(eventStream, UnbundleSubmission.class);

        final Stream<Object> events = aggregateFunction.apply(unbundleSubmission);
        final JsonEnvelope jsonEnvelope = envelopeFrom(command.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
