package uk.gov.moj.cpp.staging.prosecutors.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.domain.ApplicationSubmission;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.command.SubmitApplication;


@SuppressWarnings({"squid:pmd:NullAssignment"})
@ServiceComponent(COMMAND_HANDLER)
public class StagingProsecutorsApplicationCommandHandler {

    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;


    @Handles("stagingprosecutors.command.submit-application")
    public void handleSubmitApplication(final Envelope<SubmitApplication> envelope) throws EventStreamException {

        final SubmitApplication submitApplication = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(submitApplication.getCourtApplication().getId());
        final ApplicationSubmission applicationSubmission = aggregateService.get(eventStream, ApplicationSubmission.class);

        final UUID pocaFileId = submitApplication.getPocaFileId();
        final String senderMail = submitApplication.getSenderEmail();
        final String emailSubject = submitApplication.getEmailSubject();

        final Stream<Object> events = applicationSubmission.receiveSubmission(submitApplication.getCourtApplication(), submitApplication.getBoxHearingRequest(), pocaFileId , senderMail, emailSubject);

        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}
