package utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static utils.DateConverter.getUTCZonedDateTimeString;

import java.util.Calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DateConverterTest {

    @Test
    public void shouldConvertDateToString() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2023, 9, 17);
        final String convertedDate = getUTCZonedDateTimeString(calendar.getTime());
        assertThat(convertedDate.substring(0, 10), is("2023-10-17"));
    }


}