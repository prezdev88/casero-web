package cl.casero.migration.util;

import cl.casero.migration.domain.enums.TransactionType;

public final class TransactionTypeUtil {

    private TransactionTypeUtil() {
    }

    public static String label(TransactionType type) {
        if (type == null) {
            return "—";
        }
        return switch (type) {
            case SALE -> "Venta";
            case PAYMENT -> "Abono";
            case REFUND -> "Devolución";
            case DEBT_FORGIVENESS -> "Condonación de deuda";
            case INITIAL_BALANCE -> "Saldo inicial";
            case FAULT_DISCOUNT -> "Descuento por falla";
        };
    }
}
