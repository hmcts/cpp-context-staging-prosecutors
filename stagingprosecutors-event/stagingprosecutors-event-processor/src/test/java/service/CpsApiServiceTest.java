package service;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObject;

import dto.ResponseDto;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CpsApiServiceTest {

    @Mock
    private HttpClientWrapper httpClientWrapper;

    @InjectMocks
    private CpsApiService cpsApiService;

    @BeforeEach
    public void setUp() {
        setField(cpsApiService, "applicationNotificationUrl", "http://localhost:8080/CPS/v1/application-notification");
        setField(cpsApiService, "applicationRequestNotificationUrl", "http://localhost:8080/CPS/v1/application-request-notification");
        setField(cpsApiService, "httpClientWrapper", httpClientWrapper);
    }

    @Test
    public void shouldSendApplicationCreatedNotification() throws IOException {

        final String httpMethhod = "get";
        final JsonObject payload = Json.createObjectBuilder().build();

        final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);
        final HttpEntity entity = mock(HttpEntity.class);

        when(httpClientWrapper.createSecureHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(response);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(null);
        final ResponseDto responseDto = cpsApiService.sendApplicationCreatedNotification(payload, httpMethhod);
        assertThat(responseDto.getStatusCode(), is(SC_OK));
        assertThat(responseDto.getMessageBody(), nullValue());

    }


}