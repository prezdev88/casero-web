package cl.casero.migration.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AuditActionTest {

    @ParameterizedTest
    @EnumSource(AuditAction.class)
    void preservesPersistedIdentifiersShapesAndNullOmissionWithoutMutatingInput(AuditAction action) {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", 1L);
        data.put("year", null);
        String identifier = action.name();
        Map<String, Object> expectedData = Map.of("customerId", 1L);
        boolean flat = action == AuditAction.UPDATE_CUSTOMER_NAME || action == AuditAction.UPDATE_CUSTOMER_BIRTHDATE;
        Map<String, Object> expected = flat
                ? Map.of("action", identifier, "customerId", 1L)
                : Map.of("type", identifier, "data", expectedData);

        Map<String, Object> result = action.payload(data);

        assertThat(result).isEqualTo(expected);
        assertThat(data).containsEntry("year", null);
    }

    @Test
    void retainsLegacyFilterAliasesAndIncludesAllWrappedActions() {
        List<String> options = AuditAction.payloadTypeOptions();
        assertThat(options).startsWith("DEBT_FORGIVEN", "SALE", "PAYMENT", "REFUND", "DISCOUNT", "UPDATE")
                .contains("FAULT_DISCOUNT", "APP_CONFIG_UPDATED", "ADMIN_USER_CREATED")
                .doesNotContain("UPDATE_CUSTOMER_NAME", "UPDATE_CUSTOMER_BIRTHDATE")
                .doesNotHaveDuplicates();
    }
}
