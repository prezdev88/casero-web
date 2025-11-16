package cl.casero.migration.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Map;

public final class DateUtil {

    private static final Map<Long, String> MONTH_ABBREVIATIONS = Map.ofEntries(
            Map.entry(1L, "ene"),
            Map.entry(2L, "feb"),
            Map.entry(3L, "mar"),
            Map.entry(4L, "abr"),
            Map.entry(5L, "may"),
            Map.entry(6L, "jun"),
            Map.entry(7L, "jul"),
            Map.entry(8L, "ago"),
            Map.entry(9L, "sep"),
            Map.entry(10L, "oct"),
            Map.entry(11L, "nov"),
            Map.entry(12L, "dic")
    );

    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("dd-")
            .appendText(ChronoField.MONTH_OF_YEAR, MONTH_ABBREVIATIONS)
            .appendPattern("-yyyy")
            .toFormatter(new Locale("es", "CL"));

    private DateUtil() {
    }

    public static String format(LocalDate date) {
        return date == null ? "" : date.format(FORMATTER);
    }
}
