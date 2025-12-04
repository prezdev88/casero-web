package cl.casero.migration.service.impl;

import cl.casero.migration.domain.AppConfig;
import cl.casero.migration.repository.AppConfigRepository;
import cl.casero.migration.service.AppConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class AppConfigServiceImpl implements AppConfigService {

    private static final String AUDIT_ENABLED_KEY = "audit.logging.enabled";
    private final AppConfigRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<AppConfig> findByKey(String key) {
        return repository.findByConfigKey(key);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAuditEnabled() {
        return parseBooleanValue(repository.findByConfigKey(AUDIT_ENABLED_KEY).map(AppConfig::getValue).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppConfig> listAll() {
        return repository.findAll(Sort.by("configKey"));
    }

    @Override
    @Transactional
    public void updateValue(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("La clave de configuración es obligatoria");
        }
        String normalizedValue = value != null ? value.trim() : "";
        AppConfig config = repository.findByConfigKey(key)
            .orElseGet(() -> {
                AppConfig c = new AppConfig();
                c.setConfigKey(key);
                return c;
            });
        config.setValue(normalizedValue);
        repository.save(config);
    }

    private boolean parseBooleanValue(String raw) {
        if (raw == null) {
            return true;
        }
        String normalized = raw.trim().toLowerCase();
        switch (normalized) {
            case "true":
            case "1":
            case "yes":
            case "on":
                return true;
            case "false":
            case "0":
            case "no":
            case "off":
                return false;
            default:
                if (normalized.startsWith("{")) {
                    try {
                        JsonNode node = objectMapper.readTree(normalized);
                        if (node.has("enabled") && node.get("enabled").isBoolean()) {
                            return node.get("enabled").asBoolean();
                        }
                    } catch (Exception ignored) {
                        // fall through to default
                    }
                }
                return true;
        }
    }
}
