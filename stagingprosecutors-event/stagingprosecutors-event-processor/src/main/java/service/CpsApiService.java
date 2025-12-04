package service;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.mime.MultipartEntityBuilder.create;

import uk.gov.justice.services.common.configuration.Value;

import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.json.JsonObject;

import dto.ResponseDto;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2629", "squid:S2221"})
public class CpsApiService {

    @Inject
    @Value(key = "probationCaseWorker.subscription.key", defaultValue = "3674a16507104b749a76b29b6c837352")
    private String subscriptionKey;

    @Inject
    @Value(key = "applicationRequestNotificationUrl", defaultValue = "http://localhost:8080/CPS/v1/application-request-notification")
    private String applicationRequestNotificationUrl;

    @Inject
    @Value(key = "applicationNotificationUrl", defaultValue = "http://localhost:8080/CPS/v1/application-notification")
    private String applicationNotificationUrl;

    private final HttpClientWrapper httpClientWrapper = new HttpClientWrapper();

    private static final Logger LOGGER = LoggerFactory.getLogger(CpsApiService.class);

    @SuppressWarnings({"squid:S2139", "squid:S00112", "squid:S2142"})
    public ResponseDto sendApplicationCreatedNotification(final JsonObject payload, final String httpMethhod) {
        LOGGER.info(String.format("Sending notification to %s", applicationRequestNotificationUrl));
        final HttpEntity data = create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addBinaryBody("Notification", payload.toString().getBytes())
                .build();
        final HttpUriRequest request =  ("POST".equals(httpMethhod)) ? RequestBuilder
                .post(applicationNotificationUrl)
                .setEntity(data)
                .build()  : RequestBuilder
                .patch(applicationRequestNotificationUrl)
                .setEntity(data)
                .build();
        LOGGER.info(String.format("Executing request : %s", request.getRequestLine()));

        try (final CloseableHttpClient httpClient = httpClientWrapper.createSecureHttpClient();
             final CloseableHttpResponse response = httpClient.execute(request)) {

            final int statusCode = response.getStatusLine().getStatusCode();
            final String messageBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (statusCode != SC_OK) {
                LOGGER.error("Call to CPS notification endpoint failed with http status code: {}", statusCode );
            } else {
                LOGGER.info(String.format("Call to CPS notification endpoint successful with http status code : %s", statusCode));
            }
            return new ResponseDto(statusCode, messageBody);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }
}