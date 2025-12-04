package uk.gov.moj.cpp.staging.prosecutors.command.handler;


import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.converter.RequisitionDefendantConverter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.converter.RequisitionProsecutionSubmissionDetailsConverter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.util.StagingCaseProsecutorHandlerUtil;
import uk.gov.moj.cpp.staging.prosecutors.domain.ProsecutionSubmission;

import java.util.stream.Stream;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class SubmitRequisitionCaseCommandHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    private final RequisitionProsecutionSubmissionDetailsConverter requisitionProsecutionSubmissionDetailsConverter = new RequisitionProsecutionSubmissionDetailsConverter();

    private final RequisitionDefendantConverter requisitionDefendantConverter = new RequisitionDefendantConverter();


    @Handles("stagingprosecutors.command.requisition-prosecution")
    public void handleRequisitionProsecutionSubmission(final Envelope<SubmitRequisitionProsecution> envelope) throws EventStreamException {

        final SubmitRequisitionProsecution submitRequisitionProsecution = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(submitRequisitionProsecution.getSubmissionId());
        final ProsecutionSubmission prosecutionSubmission = aggregateService.get(eventStream, ProsecutionSubmission.class);

        final Stream<Object> events = prosecutionSubmission.receiveSubmission(
                submitRequisitionProsecution.getSubmissionId(),
                requisitionProsecutionSubmissionDetailsConverter.convert(submitRequisitionProsecution.getProsecutionSubmissionDetails()),
                requisitionDefendantConverter.convert(submitRequisitionProsecution.getDefendants()));

        StagingCaseProsecutorHandlerUtil.appendEventsToStream(envelope, eventStream, events);
    }

}
