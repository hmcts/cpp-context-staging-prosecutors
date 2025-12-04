package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.moj.cpp.staging.prosecutorapi.stub.NotifyStub.stubNotificationForEmail;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.fileservice.FileUtil.getDocumentBytesFromFile;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.fileservice.FileUtil.getStringFromResource;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.fileservice.FileUtil.jsonFromString;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.fileservice.FileServiceClient;

import java.sql.SQLException;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public class PocaEmailNotificationIT {

    public static final UUID USER_ID = UUID.randomUUID();
    public static final String CONTEXT_NAME = "stagingprosecutors";
    private static final String PUBLIC_NOTIFICATION_SENT = "public.notificationnotify.events.poca-email-notification-received";
    private final JmsMessageProducerClient PUBLIC_MESSAGE_PRODUCER = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String STAGINGPROSECUTORS_EVENT_APPLICATION_SUBMITTED = "stagingprosecutors.event.application-submitted";
    private static final String STAGINGPROSECUTORS_EVENT_APPLICATION_NOT_VALIDATED = "stagingprosecutors.event.poca-document-not-validated";

    public static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    final private WiremockUtils wiremockUtils = new WiremockUtils();

    static {
        configureFor(HOST, 8080);
        stubNotificationForEmail();
    }

    @BeforeEach
    public void setUpClass() {
        new WiremockUtils().stubAccessControl(true, USER_ID, "CPPI Consumers");
    }

    @Test
    public void shouldReceivePocaEmailNotificationForIndividual() throws FileServiceException, SQLException {
        wiremockUtils.stubReferenceDataCourtApplicationTypes();
        wiremockUtils.stubReferenceDataPublicHolidays();

        final byte[] documentContent = getDocumentBytesFromFile("docx/iw018-eng-individual-respondent-fields.docx");
        final UUID fileStoreId = FileServiceClient.create("iw018-eng-individual-respondent-fields.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", documentContent);

        final String payloadUpdatedStr = getStringFromResource("public.notificationnotify.poca-email-notification-received.json").replaceAll("POCA_FILE_ID", fileStoreId.toString());

        produceNotificationSentPublicEvent(jsonFromString(payloadUpdatedStr));
        final JmsMessageConsumerClient messageConsumerApplicationSubmittedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(STAGINGPROSECUTORS_EVENT_APPLICATION_SUBMITTED).getMessageConsumerClient();
        assertThat(STAGINGPROSECUTORS_EVENT_APPLICATION_SUBMITTED + " message not found in defence.event topic", messageConsumerApplicationSubmittedPrivateEvent.retrieveMessage().isPresent(), is(true));

    }

    @Test
    public void shouldReceivePocaEmailNotificationForOrganisation() throws FileServiceException, SQLException {
        wiremockUtils.stubReferenceDataCourtApplicationTypes();
        wiremockUtils.stubReferenceDataPublicHolidays();

        final byte[] documentContent = getDocumentBytesFromFile("docx/iw018-eng-organisation-respondent-fields.docx");
        final UUID fileStoreId = FileServiceClient.create("iw018-eng-organisation-respondent-fields.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", documentContent);

        final String payloadUpdatedStr = getStringFromResource("public.notificationnotify.poca-email-notification-received.json").replaceAll("POCA_FILE_ID", fileStoreId.toString());

        produceNotificationSentPublicEvent(jsonFromString(payloadUpdatedStr));
        final JmsMessageConsumerClient messageConsumerClient = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(STAGINGPROSECUTORS_EVENT_APPLICATION_SUBMITTED).getMessageConsumerClient();
        assertThat(STAGINGPROSECUTORS_EVENT_APPLICATION_SUBMITTED + " message not found in defence.event topic", messageConsumerClient.retrieveMessage(80000L).isPresent(), is(true));

    }

    @Test
    public void shouldReceivePocaEmailNotificationForFailureNotification() {
        final String payloadUpdatedStr = getStringFromResource("public.notificationnotify.poca-email-notification-received-1.json").replaceAll("POCA_FILE_ID", UUID.randomUUID().toString());

        produceNotificationSentPublicEvent(jsonFromString(payloadUpdatedStr));
        final JmsMessageConsumerClient messageConsumerClient = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(STAGINGPROSECUTORS_EVENT_APPLICATION_NOT_VALIDATED).getMessageConsumerClient();
        assertThat(STAGINGPROSECUTORS_EVENT_APPLICATION_NOT_VALIDATED + " message not found in defence.event topic", messageConsumerClient.retrieveMessage(80000L).isPresent(), is(true));

    }

    private void produceNotificationSentPublicEvent(final JsonObject payload) {
        final Metadata metadata = metadataBuilder().withId(randomUUID()).withUserId(USER_ID.toString()).withName(PUBLIC_NOTIFICATION_SENT).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);
        PUBLIC_MESSAGE_PRODUCER.sendMessage(PUBLIC_NOTIFICATION_SENT, jsonEnvelope);
    }

}
