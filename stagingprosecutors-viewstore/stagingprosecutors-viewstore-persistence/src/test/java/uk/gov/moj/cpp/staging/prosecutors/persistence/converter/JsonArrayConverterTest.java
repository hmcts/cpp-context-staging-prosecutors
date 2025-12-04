package uk.gov.moj.cpp.staging.prosecutors.persistence.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class JsonArrayConverterTest extends BaseTransactionalTest {

    @Inject
    private JsonArrayConverter jsonArrayConverter;

    @Test
    public void shouldConvertToDatabaseColumn() {
        final JsonArray array = Json.createArrayBuilder()
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
