package cl.casero.migration.domain.enums;

public enum TransactionType {
    SALE,
    PAYMENT,
    REFUND,
    DEBT_FORGIVENESS,
    INITIAL_BALANCE;

    public boolean isDebtDecreaser() {
        return this == PAYMENT || this == REFUND || this == DEBT_FORGIVENESS;
    }

    public static TransactionType fromLegacyId(int id) {
        switch (id) {
            case 0:
                return SALE;
            case 1:
                return PAYMENT;
            case 2:
                return REFUND;
            case 3:
                return DEBT_FORGIVENESS;
            case 4:
                return INITIAL_BALANCE;
            default:
                throw new IllegalArgumentException("Unsupported legacy transaction type id: " + id);
        }
    }

    public String getDisplayName() {
        switch (this) {
            case SALE:
                return "Venta";
            case PAYMENT:
                return "Abono";
            case REFUND:
                return "Devolución";
            case DEBT_FORGIVENESS:
                return "Condonación de deuda";
            case INITIAL_BALANCE:
                return "Saldo inicial";
            default:
                return name();
        }
    }
}
