package cl.casero.migration.util;

import cl.casero.migration.domain.enums.TransactionType;

public final class TransactionTypeUtil {

    private TransactionTypeUtil() {}

    public static String label(TransactionType type) {
        if (type == null) {
            return "—";
        }

        TransactionTypePresentation presentation = TransactionTypePresentation.forType(type);
        return presentation.getLabel();
    }
}
