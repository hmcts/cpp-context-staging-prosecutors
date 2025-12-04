package uk.gov.moj.cpp.staging.prosecutors.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.converter.SummonsDefendantConverter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.converter.SummonsProsecutionSubmissionDetailsConverter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.util.StagingCaseProsecutorHandlerUtil;
import uk.gov.moj.cpp.staging.prosecutors.domain.ProsecutionSubmission;

import java.util.stream.Stream;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class SubmitSummonsCaseCommandHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    private final SummonsProsecutionSubmissionDetailsConverter summonsProsecutionSubmissionDetailsConverter = new SummonsProsecutionSubmissionDetailsConverter();

    private final SummonsDefendantConverter summonsDefendantConverter = new SummonsDefendantConverter();

    @Handles("stagingprosecutors.command.summons-prosecution")
    public void handleSummonsProsecutionSubmission(final Envelope<SubmitSummonsProsecution> envelope) throws EventStreamException {

        final SubmitSummonsProsecution submitSummonsProsecution = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(submitSummonsProsecution.getSubmissionId());
        final ProsecutionSubmission prosecutionSubmission = aggregateService.get(eventStream, ProsecutionSubmission.class);

        final Stream<Object> events = prosecutionSubmission.receiveSubmission(
                submitSummonsProsecution.getSubmissionId(),
                summonsProsecutionSubmissionDetailsConverter.convert(submitSummonsProsecution.getProsecutionSubmissionDetails()),
                summonsDefendantConverter.convert(submitSummonsProsecution.getDefendants()));

        StagingCaseProsecutorHandlerUtil.appendEventsToStream(envelope, eventStream, events);
    }

}
