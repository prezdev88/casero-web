package cl.casero.migration.service.impl;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.casero.migration.domain.AppConfig;
import cl.casero.migration.service.AppConfigService;
import cl.casero.migration.service.AuditPolicy;

@Service
@RequiredArgsConstructor
public class ConfiguredAuditPolicy implements AuditPolicy {

    private static final String AUDIT_ENABLED_KEY = "audit.logging.enabled";

    private final AppConfigService configuration;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public boolean isAuditEnabled() {
        Optional<AppConfig> config = configuration.findByKey(AUDIT_ENABLED_KEY);
        String rawValue = config.map(AppConfig::getValue).orElse(null);
        if (rawValue == null) {
            return true;
        }

        String normalized = rawValue.trim().toLowerCase();
        return switch (normalized) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> normalized.startsWith("{") ? parseJsonPolicy(normalized) : true;
        };
    }

    private boolean parseJsonPolicy(String value) {
        try {
            JsonNode node = objectMapper.readTree(value);
            JsonNode enabled = node.get("enabled");
            if (enabled != null && enabled.isBoolean()) {
                return enabled.asBoolean();
            }

            return true;
        } catch (Exception ignored) {
            return true;
        }
    }
}
