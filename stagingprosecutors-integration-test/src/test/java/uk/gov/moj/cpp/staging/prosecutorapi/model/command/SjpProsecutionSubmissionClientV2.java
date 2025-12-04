package uk.gov.moj.cpp.staging.prosecutorapi.model.command;

import static uk.gov.moj.cpp.staging.prosecutorapi.it.Constants.PRIVATE_SJP_PROSECUTION_RECEIVED;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.OUCODE;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Defendant;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.ProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.commandclient.CommandClient;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.commandclient.CommandExecutor;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.commandclient.EventHandler;

import java.util.function.Consumer;

import lombok.Builder;

@Builder
@CommandClient(
        URI = "/v2/prosecutions/" + OUCODE,
        contentType = "application/vnd.hmcts.cjs.sjp-prosecution.v2+json"
)
public class SjpProsecutionSubmissionClientV2 {

    @Builder.Default
    public ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder().build();

    @Builder.Default
    public Defendant defendant = Defendant.builder().build();

    @Builder.Default
    public String additionalProperty = null;

    @EventHandler(PRIVATE_SJP_PROSECUTION_RECEIVED)
    public Consumer<JsonEnvelope> caseReceivedHandler;

    public CommandExecutor getExecutor() {
        return new CommandExecutor(this);
    }

}
