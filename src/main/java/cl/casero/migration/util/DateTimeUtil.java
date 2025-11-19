package cl.casero.migration.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateTimeUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("dd-MMM-uuuu HH:mm")
            .withLocale(new Locale("es", "CL"));

    private DateTimeUtil() {
    }

    public static String format(OffsetDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(FORMATTER);
    }
}
