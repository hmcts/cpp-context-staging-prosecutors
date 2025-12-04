package utils;

import static java.time.ZoneOffset.UTC;
import static java.util.TimeZone.getTimeZone;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class DateConverter {

    private static final DateTimeZone UTC_DATE_TIME_ZONE = DateTimeZone.forTimeZone(getTimeZone(UTC));

    private DateConverter() {
    }

    public static String getUTCZonedDateTimeString(final Date date) {
        return new DateTime(date, UTC_DATE_TIME_ZONE).toString();
    }

}
