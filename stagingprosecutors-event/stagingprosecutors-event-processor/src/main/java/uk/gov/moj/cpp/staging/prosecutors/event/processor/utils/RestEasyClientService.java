package uk.gov.moj.cpp.staging.prosecutors.event.processor.utils;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import uk.gov.justice.services.common.configuration.Value;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

@SuppressWarnings({"squid:S2139", "squid:S00112", "squid:S2142"})
public class RestEasyClientService {

    public static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
    public static final String OCP_APIM_TRACE = "Ocp-Apim-Trace";
    public static final String TRUE = "true";

    ResteasyClient client;

    @Inject
    @Value(key = "restEasyClientConnectionPoolSize", defaultValue = "10")
    private String restEasyClientConnectionPoolSize;

    @PostConstruct
    public void createClient() {
        client = new ResteasyClientBuilderImpl().disableTrustManager()
                .connectionPoolSize(Integer.parseInt(restEasyClientConnectionPoolSize))
                .build();
    }

    public Response post(final String url, final String payload, final String key) {
        final Invocation.Builder request = this.client.target(url).request();
        request.headers(new MultivaluedHashMap(getHeaders(key)));
        return request.post(Entity.json(payload));
    }

    public Response patch(final String url, final String payload, final String key, final String mediaType) {
        final Invocation.Builder request = this.client.target(url).request();
        request.headers(new MultivaluedHashMap(getHeaders(key, mediaType)));
        return request.method("PATCH", Entity.json(payload));
    }

    private Map<String, String> getHeaders(final String subscriptionKey) {
        return ImmutableMap.of(
                ACCEPT, MediaType.APPLICATION_JSON,
                OCP_APIM_SUBSCRIPTION_KEY, subscriptionKey,
                OCP_APIM_TRACE, TRUE);
    }

    private Map<String, String> getHeaders(final String subscriptionKey, final String mediaType) {
        return ImmutableMap.of(
                ACCEPT, mediaType,
                OCP_APIM_SUBSCRIPTION_KEY, subscriptionKey,
                OCP_APIM_TRACE, TRUE);
    }
}
