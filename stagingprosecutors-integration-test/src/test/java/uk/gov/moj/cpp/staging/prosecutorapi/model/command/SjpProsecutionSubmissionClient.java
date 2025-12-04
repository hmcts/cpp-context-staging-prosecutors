package uk.gov.moj.cpp.staging.prosecutorapi.model.command;

import static uk.gov.moj.cpp.staging.prosecutorapi.it.Constants.PRIVATE_SJP_PROSECUTION_RECEIVED;

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
        URI = "/v1/prosecutions",
        contentType = "application/vnd.hmcts.cjs.sjp-prosecution+json"
)
public class SjpProsecutionSubmissionClient {

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
