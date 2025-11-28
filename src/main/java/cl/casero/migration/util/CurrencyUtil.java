package cl.casero.migration.util;

import java.text.NumberFormat;
import java.util.Locale;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class CurrencyUtil {

    private static final NumberFormat FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));

    public static String format(int amount) {
        return FORMAT.format(amount);
    }
}
