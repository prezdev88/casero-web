package cl.casero.migration.domain.enums;

public enum TransactionType {
    
    SALE,
    PAYMENT,
    REFUND,
    DEBT_FORGIVENESS,
    INITIAL_BALANCE,
    FAULT_DISCOUNT;

    public boolean isDebtDecreaser() {
        return this == PAYMENT || this == REFUND || this == DEBT_FORGIVENESS || this == FAULT_DISCOUNT;
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
            case 5:
                return FAULT_DISCOUNT;
            default:
                throw new IllegalArgumentException("Unsupported legacy transaction type id: " + id);
        }
    }

}
