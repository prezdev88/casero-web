package cl.casero.migration.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateUtil {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy", new Locale("es", "CL"));

    private DateUtil() {
    }

    public static String format(LocalDate date) {
        return date == null ? "" : date.format(FORMATTER);
    }
}
