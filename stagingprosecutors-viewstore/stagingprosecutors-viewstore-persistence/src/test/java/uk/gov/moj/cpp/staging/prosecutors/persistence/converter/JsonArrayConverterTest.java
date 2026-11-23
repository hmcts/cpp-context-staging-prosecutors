package uk.gov.moj.cpp.staging.prosecutors.persistence.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;

import jakarta.json.JsonArray;

import org.junit.jupiter.api.Test;

public class JsonArrayConverterTest {

    private final JsonArrayConverter jsonArrayConverter = new JsonArrayConverter();

    @Test
    public void shouldConvertToDatabaseColumn() {
        final JsonArray array = createArrayBuilder()
                .add("value1")
                .add("value2")
                .build();
        final String result = jsonArrayConverter.convertToDatabaseColumn(array);
        assertThat(result, is("[\"value1\",\"value2\"]"));
    }

    @Test
    public void shouldConvertToEntityAttribute() {
        final JsonArray result = jsonArrayConverter.convertToEntityAttribute("[\"value1\",\"value2\"]");
        assertThat(result.getString(0), is("value1"));
        assertThat(result.getString(1), is("value2"));
    }
}
