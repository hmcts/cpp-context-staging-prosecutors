package uk.gov.moj.cpp.staging.prosecutors.test.utils;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HandlerTestHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    private HandlerTestHelper() {
    }

    //TODO: refactor into matcher (needs techpod support)
    public static void matchEvent(final Stream<JsonEnvelope> jsonEnvelopeStream,
                                  final String eventName,
                                  final JsonValue expectedResultPayload) {

        boolean matched = false;

        final List<JsonEnvelope> collect = jsonEnvelopeStream.collect(Collectors.toList());
        for (final JsonEnvelope jsonEnvelope : collect) {
            if (jsonEnvelope.metadata().name().equals(eventName)) {
                matched = true;
                final JsonNode actualEvent = generatedEventAsJsonNode(jsonEnvelope.payloadAsJsonObject());
                assertThat(actualEvent, equalTo(generatedEventAsJsonNode(expectedResultPayload)));
                break;
            }
        }
        assertThat(matched, equalTo(true));
    }

    public static void matchEvent(final Stream<JsonEnvelope> jsonEnvelopeStream,
                                  final String eventName,
                                  final List<JsonValue> expectedResultPayloads) {

        final List<JsonEnvelope> collect = jsonEnvelopeStream.collect(Collectors.toList());
        int idx = 0;
        for (final JsonEnvelope jsonEnvelope : collect) {
            if (jsonEnvelope.metadata().name().equals(eventName)) {
                final JsonNode actualEvent = generatedEventAsJsonNode(jsonEnvelope.payloadAsJsonObject());
                assertThat(actualEvent, equalTo(generatedEventAsJsonNode(expectedResultPayloads.get(idx))));
                idx++;
            }
        }

        assertThat(collect.size(),  equalTo(idx));
    }

    public static JsonNode matchEvent(final Stream<JsonEnvelope> jsonEnvelopeStream,
                                  final String eventName) {


        final List<JsonEnvelope> collect = jsonEnvelopeStream.collect(Collectors.toList());
        for (final JsonEnvelope jsonEnvelope : collect) {
            if (jsonEnvelope.metadata().name().equals(eventName)) {
                return generatedEventAsJsonNode(jsonEnvelope.payloadAsJsonObject());
            }
        }

        fail();
        return null;
    }

    public static Metadata metadataFor(final String commandName, final UUID commandId) {
        return metadataBuilder()
                .withName(commandName)
                .withId(commandId)
                .withUserId(randomUUID().toString())
                .build();
    }

    public static Metadata metadataFor(final String commandName) {
        return metadataBuilder()
                .withName(commandName)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();
    }

    public static JsonNode generatedEventAsJsonNode(final Object generatedEvent) {
        return OBJECT_MAPPER.valueToTree(generatedEvent);
    }
}
