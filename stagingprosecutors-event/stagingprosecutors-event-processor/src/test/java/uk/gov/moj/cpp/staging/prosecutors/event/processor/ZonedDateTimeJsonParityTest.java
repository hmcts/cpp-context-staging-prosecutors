package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.staging.prosecutors.domain.DocumentUnbundledV2;

import java.io.StringReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import org.junit.jupiter.api.Test;

/**
 * J17/J25 parity characterisation (J17 golden master), SP-M06 / BC-08.
 *
 * Jackson moves from 2.12 to 2.21 (jsr310 / jakarta-jsonp modules). Pin how the framework mapper writes and reads
 * the ZonedDateTime carried by the document-unbundled-v2 event, and how the framework reads metadata createdAt, which
 * the listener stores as submission.received_at / completed_at. A change in zone identity (ZoneOffset.UTC vs the
 * UTC region) changes ZonedDateTime equality and toString, which the query view returns as receivedAt.
 */
class ZonedDateTimeJsonParityTest {

    private static final UUID CASE_ID = UUID.fromString("6f0e2b5a-8a7c-4c1d-9b0e-2f1d3c4b5a69");

    private final ObjectMapperProducer objectMapperProducer = new ObjectMapperProducer();

    @Test
    void spM06_shouldWriteReceivedDateTimeInTheJ17Format() {
        final DocumentUnbundledV2 event = new DocumentUnbundledV2(CASE_ID, emptyList(), empty(), "DEF-1",
                ZonedDateTime.of(2024, 6, 1, 10, 0, 0, 0, ZoneId.of("Europe/London")));

        final JsonObject json = new ObjectToJsonObjectConverter(objectMapperProducer.objectMapper()).convert(event);

        assertThat(json.getString("receivedDateTime"), is("2024-06-01T09:00:00.000Z"));
    }

    // J17 (Jackson 2.12) read this into the UTC region ("...Z[UTC]"); Jackson 2.21 reads ZoneOffset.UTC. Accepted: the
    // only output use formats receivedDateTime with a literal 'Z' pattern, which gives the same string for both.
    @Test
    void spM06_shouldReadReceivedDateTimeAsZoneOffsetUtc() {
        final JsonObject json = parse("{\"caseId\":\"" + CASE_ID + "\",\"materials\":[],\"prosecutorDefendantId\":\"DEF-1\","
                + "\"receivedDateTime\":\"2024-06-01T09:00:00.000Z\"}");

        final DocumentUnbundledV2 event = new JsonObjectToObjectConverter(objectMapperProducer.objectMapper()).convert(json, DocumentUnbundledV2.class);

        assertThat(event.getReceivedDateTime().toString(), is("2024-06-01T09:00Z"));
    }

    @Test
    void spM06_shouldReadMetadataCreatedAtWithTheJ17Zone() {
        final JsonObject metadata = parse("{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"stagingprosecutors.event.prosecution-received\","
                + "\"createdAt\":\"2024-06-01T09:00:00.000Z\"}");

        final ZonedDateTime createdAt = metadataFrom(metadata).build().createdAt().get();

        assertThat(createdAt.toString(), is("2024-06-01T09:00Z"));
    }

    private static JsonObject parse(final String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }
}
