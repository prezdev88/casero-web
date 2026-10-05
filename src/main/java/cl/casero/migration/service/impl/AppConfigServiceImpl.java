package cl.casero.migration.service.impl;

import cl.casero.migration.domain.AppConfig;
import cl.casero.migration.repository.AppConfigRepository;
import cl.casero.migration.service.AppConfigService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class AppConfigServiceImpl implements AppConfigService {

    private final AppConfigRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<AppConfig> findByKey(String key) {
        return repository.findByConfigKey(key);
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
}
