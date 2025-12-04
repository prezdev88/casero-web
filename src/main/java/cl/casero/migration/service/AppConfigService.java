package cl.casero.migration.service;

import java.util.Optional;
import cl.casero.migration.domain.AppConfig;
import java.util.List;

public interface AppConfigService {
    Optional<AppConfig> findByKey(String key);

    boolean isAuditEnabled();

    List<AppConfig> listAll();

    void updateValue(String key, String value);
}
