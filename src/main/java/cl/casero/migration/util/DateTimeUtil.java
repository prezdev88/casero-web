package cl.casero.migration.util;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateTimeUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("dd-MMM-uuuu HH:mm")
            .withLocale(new Locale("es", "CL"));
            
    private static final ZoneId SANTIAGO_ZONE = ZoneId.of("America/Santiago");

    private DateTimeUtil() {}

    public static String format(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        return dateTime.atZoneSameInstant(SANTIAGO_ZONE).format(FORMATTER);
    }
}
