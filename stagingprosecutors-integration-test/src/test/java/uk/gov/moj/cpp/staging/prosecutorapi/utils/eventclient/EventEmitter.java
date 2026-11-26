package uk.gov.moj.cpp.staging.prosecutorapi.utils.eventclient;

import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.exception.ConverterException;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

public class EventEmitter {

    @Getter
    private ObjectMapper mapper = new ObjectMapperProducer().objectMapper().copy();

    @Getter
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    private final Object eventInstance;

    private UUID correlationId = UUID.randomUUID();

    private UUID userId = UUID.randomUUID();

    private Map<String, String> customMetadata = new HashMap<>();

    public EventEmitter(Object eventInstance) {
        if (!eventInstance.getClass().isAnnotationPresent(Event.class)) {
            throw new IllegalArgumentException("Event emitter can only be used with the classes decorated with Event annotation.");
        }

        this.eventInstance = eventInstance;
    }

    public EventEmitter setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    public EventEmitter setExecutingUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public EventEmitter addCustomMetadata(String key, String value) {
        customMetadata.put(key, value);
        return this;
    }

    public void emit() {

        final String eventName = eventInstance.getClass().getAnnotation(Event.class).name();
        final JsonObject payload = convertToJsonObject(eventInstance);
        final Metadata metadata = createMetadata();

        final JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        publicMessageProducerClient.sendMessage(eventName, envelopeFrom(metadata, payload));
    }

    private Metadata createMetadata() {
        final JsonObjectBuilder metadataBuilder = JsonObjects.createObjectBuilder(
                metadataBuilder()
                        .withClientCorrelationId(correlationId.toString())
                        .withName(eventInstance.getClass().getAnnotation(Event.class).name())
                        .withUserId(userId.toString())
                        .withId(UUID.randomUUID())
                        .createdAt(new UtcClock().now()).build()
                        .asJsonObject()
        );

        this.customMetadata.entrySet().forEach((entry) -> metadataBuilder.add(entry.getKey(), entry.getValue()));
        return metadataFrom(metadataBuilder.build()).build();
    }

    private JsonObject convertToJsonObject(Object source) {
        try {
            JsonObject jsonObject = this.mapper.readValue(this.mapper.writeValueAsString(source), JsonObject.class);
            if (jsonObject == null) {
                throw new ConverterException(String.format("Failed to convert %s to JsonObject", source));
            } else {
                return jsonObject;
            }
        } catch (IOException var3) {
            throw new IllegalArgumentException(String.format("Error while converting %s toJsonObject", source), var3);
        }
    }

}
