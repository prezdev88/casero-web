package cl.casero.migration.util;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import cl.casero.migration.domain.enums.TransactionType;

@Getter
@RequiredArgsConstructor
public enum TransactionTypePresentation {

    SALE("Venta", "🛒"),
    PAYMENT("Abono", "💰"),
    REFUND("Devolución", "↩️"),
    DEBT_FORGIVENESS("Condonación de deuda", "❤️"),
    INITIAL_BALANCE("Saldo inicial", "⚖️"),
    FAULT_DISCOUNT("Descuento por falla", "🛠️");

    private final String label;
    private final String icon;

    public static TransactionTypePresentation forType(TransactionType type) {
        String key = type.name();
        return valueOf(key);
    }

    public static Map<TransactionType, TransactionTypePresentation> byType() {
        Map<TransactionType, TransactionTypePresentation> metadata = new EnumMap<>(TransactionType.class);
        for (TransactionType type : TransactionType.values()) {
            TransactionTypePresentation presentation = forType(type);
            metadata.put(type, presentation);
        }

        return Collections.unmodifiableMap(metadata);
    }

    public static Map<String, Map<String, String>> byKey() {
        Map<String, Map<String, String>> metadata = new LinkedHashMap<>();
        for (TransactionTypePresentation presentation : values()) {
            String name = presentation.name();
            String key = name.toLowerCase(Locale.ROOT);
            Map<String, String> details = Map.of("label", presentation.label, "icon", presentation.icon);
            metadata.put(key, details);
        }

        return Collections.unmodifiableMap(metadata);
    }
}
