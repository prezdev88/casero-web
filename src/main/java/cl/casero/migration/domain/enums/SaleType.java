package cl.casero.migration.domain.enums;

public enum SaleType {
    NEW_SALE(0),
    MAINTENANCE(1);

    private final int legacyId;

    SaleType(int legacyId) {
        this.legacyId = legacyId;
    }

    public int getLegacyId() {
        return legacyId;
    }

    public static SaleType fromLegacyId(int id) {
        for (SaleType type : values()) {
            if (type.legacyId == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported legacy sale type id: " + id);
    }
}
