package uk.gov.moj.cpp.staging.prosecutors.event.processor.jobstore.tasks;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo.executionInfo;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.COMPLETED;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.jobstore.tasks.TaskNames.CASE_FILE_ADD_MATERIAL_TASK;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.jobstore.api.annotation.Task;
import uk.gov.moj.cpp.jobstore.api.task.ExecutableTask;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;

import javax.inject.Inject;

@Task(CASE_FILE_ADD_MATERIAL_TASK)
public class CaseFileAddMaterialTask implements ExecutableTask
{

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;



    @Override
    public ExecutionInfo execute(final ExecutionInfo executionInfo) {
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataFrom(executionInfo.getJobData().getJsonObject("metadata")),
                executionInfo.getJobData().getJsonObject("payload"));

        sender.sendAsAdmin(Envelope.envelopeFrom(jsonEnvelope.metadata(), jsonEnvelope.payload()));

        return executionInfo()
                .withExecutionStatus(COMPLETED)
                .build();
    }
}
