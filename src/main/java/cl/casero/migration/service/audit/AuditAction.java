package cl.casero.migration.service.audit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public enum AuditAction {

    DEBT_FORGIVEN,
    SALE,
    PAYMENT,
    REFUND,
    DISCOUNT,
    UPDATE,
    CREATE_CUSTOMER,
    FAULT_DISCOUNT,
    UPDATE_CUSTOMER_ADDRESS,
    UPDATE_CUSTOMER_SECTOR,
    DELETE_CUSTOMER,
    TRANSACTION_DELETED,
    ADMIN_USER_CREATED,
    ADMIN_USER_PIN_UPDATED,
    APP_CONFIG_UPDATED,
    UPDATE_CUSTOMER_NAME(true),
    UPDATE_CUSTOMER_BIRTHDATE(true);

    private final boolean flatPayload;

    AuditAction() {
        this(false);
    }

    AuditAction(boolean flatPayload) {
        this.flatPayload = flatPayload;
    }

    public Map<String, Object> payload(Map<String, Object> data) {
        Map<String, Object> copiedData = (data == null) ? new HashMap<>() : new HashMap<>(data);
        copiedData.values().removeIf(Objects::isNull);
        String identifier = name();
        if (flatPayload) {
            copiedData.put("action", identifier);
            return copiedData;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", identifier);
        payload.put("data", copiedData);
        return payload;
    }

    public static List<String> payloadTypeOptions() {
        AuditAction[] actions = values();
        return Arrays.stream(actions)
                .filter(action -> !action.flatPayload)
                .map(AuditAction::name)
                .toList();
    }
}
