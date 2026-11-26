package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.ResourcesUtils;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;

import java.util.UUID;

import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public class SubmitApplicationIT {

    private final UUID userId = randomUUID();
    public static final String CONTEXT_NAME = "stagingprosecutors";
    private static final String SUBMIT_APPLICATION_COMMAND_URL = getBaseUri() + "/stagingprosecutors-command-api/command/api/rest/stagingprosecutors/SubmitApplication";
    private static final String STAGINGPROSECUTORS_EVENT_APPLICATION_SUBMITTED = "stagingprosecutors.event.application-submitted";

    @BeforeEach
    public void setUpClass() {
        new WiremockUtils()
                .stubAccessControl(true, userId, "CPPI Consumers");
    }

    @Test
    public void shouldPerformApplicationSubmit() {
        final UUID applicationId = randomUUID();
        final JmsMessageConsumerClient messageConsumerClient = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(STAGINGPROSECUTORS_EVENT_APPLICATION_SUBMITTED).getMessageConsumerClient();
        postSubmitApplicationCommand("application/stagingprosecutors.submit-application-command.json", applicationId);
        assertThat(STAGINGPROSECUTORS_EVENT_APPLICATION_SUBMITTED + " message not found in defence.event topic", messageConsumerClient.retrieveMessage().isPresent(), is(true));
    }

    private void postSubmitApplicationCommand(final String resourceName, final UUID applicationId) {
        final String payload = ResourcesUtils.readResource(resourceName).replaceAll("APPLICATION_ID", applicationId.toString());

        new RestClient().postCommand(SUBMIT_APPLICATION_COMMAND_URL,
                "application/vnd.stagingprosecutors.submit-application+json",
                payload,
                createHttpHeaders(userId.toString())
        );
    }

    private MultivaluedMap<String, Object> createHttpHeaders(final String userId) {
        MultivaluedMap<String, Object> headers = new MultivaluedMapImpl<>();
        headers.add(HeaderConstants.USER_ID, userId);
        return headers;
    }
}
