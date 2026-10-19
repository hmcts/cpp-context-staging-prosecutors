package uk.gov.moj.cpp.staging.prosecutorapi.query.api.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.Test;

/**
 * J17/J25 parity characterisation (J17 golden master), SP-M03 / BC-11.
 *
 * The J25 upgrade swaps Json.createObjectBuilder()/createArrayBuilder() (Glassfish javax.json) for the framework
 * JsonObjects builders (Parsson). Pin the exact serialised v1 results response for a payload that carries the
 * values a builder is most likely to treat differently: JSON null (top level, nested, in arrays), decimals with
 * trailing zeros, large integers, booleans, escaped and non-ASCII strings, and key order.
 */
class ResultsV1ResponseTransformerParityTest {

    private static final String INPUT = "{"
            + "\"prosecutionAuthorityCode\":\"TFL\","
            + "\"nullAtTopLevel\":null,"
            + "\"cases\":[{"
            + "\"caseUrn\":\"TFL12345678\","
            + "\"amount\":12.50,"
            + "\"bigNumber\":12345678901234567890,"
            + "\"negative\":-0.0,"
            + "\"flag\":true,"
            + "\"text\":\"say \\\"hi\\\" / caf\u00e9 \\u00e9\","
            + "\"defendants\":[{"
            + "\"offences\":["
            + "{\"offenceCode\":\"PS90010\",\"verdict\":{\"verdictCode\":\"G\",\"verdictDate\":null},\"plea\":null},"
            + "{\"offenceCode\":\"PS90011\",\"verdict\":{\"verdictDate\":\"2024-06-01\"}},"
            + "{\"offenceCode\":\"PS90012\"}"
            + "],"
            + "\"mixed\":[null,1,\"two\",false,[null,{\"verdict\":{\"verdictCode\":\"N\"}}]]"
            + "}]"
            + "}]"
            + "}";

    private static final String EXPECTED = "{"
            + "\"prosecutionAuthorityCode\":\"TFL\","
            + "\"nullAtTopLevel\":null,"
            + "\"cases\":[{"
            + "\"caseUrn\":\"TFL12345678\","
            + "\"amount\":12.50,"
            + "\"bigNumber\":12345678901234567890,"
            + "\"negative\":0.0," // Glassfish writes the parsed -0.0 as 0.0
            + "\"flag\":true,"
            + "\"text\":\"say \\\"hi\\\" / caf\u00e9 \u00e9\","
            + "\"defendants\":[{"
            + "\"offences\":["
            + "{\"offenceCode\":\"PS90010\",\"plea\":null,\"verdictCode\":\"G\"},"
            + "{\"offenceCode\":\"PS90011\"},"
            + "{\"offenceCode\":\"PS90012\"}"
            + "],"
            + "\"mixed\":[null,1,\"two\",false,[null,{\"verdictCode\":\"N\"}]]"
            + "}]"
            + "}]"
            + "}";

    private final ResultsV1ResponseTransformer transformer = new ResultsV1ResponseTransformer();

    @Test
    void spM03_shouldSerialiseTheJ17ResultsV1Response() {
        assertThat(transformer.transform(parse(INPUT)).toString(), is(EXPECTED));
    }

    private static JsonObject parse(final String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }
}
