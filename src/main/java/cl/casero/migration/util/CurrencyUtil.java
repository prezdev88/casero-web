package cl.casero.migration.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyUtil {

    private static final NumberFormat FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));

    private CurrencyUtil() {
    }

    public static String format(int amount) {
        return FORMAT.format(amount);
    }
}
