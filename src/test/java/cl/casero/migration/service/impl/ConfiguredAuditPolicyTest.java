package cl.casero.migration.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.casero.migration.domain.AppConfig;
import cl.casero.migration.service.AppConfigService;

@ExtendWith(MockitoExtension.class)
class ConfiguredAuditPolicyTest {

    private static final String CONFIG_KEY = "audit.logging.enabled";

    @Mock
    private AppConfigService configuration;

    private ConfiguredAuditPolicy policy;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        policy = new ConfiguredAuditPolicy(configuration, mapper);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "NULL|true", "true|true", "TRUE|true", "' 1 '|true", "yes|true", "ON|true",
        "false|false", "FALSE|false", "0|false", "no|false", "off|false",
        "''|true", "unknown|true", "'{\"enabled\":false}'|false",
        "'{\"ENABLED\":FALSE}'|false", "'{\"enabled\":true}'|true",
        "'{\"enabled\":\"false\"}'|true", "'{\"enabled\":0}'|true",
        "'{\"other\":false}'|true", "'{enabled'|true"
    }, delimiter = '|', nullValues = "NULL")
    void preservesBooleanAliasesJsonNormalizationAndTheEnabledDefault(String raw, boolean expected) {
        AppConfig config = new AppConfig();
        config.setValue(raw);
        Optional<AppConfig> result = Optional.of(config);
        doReturn(result).when(configuration).findByKey(CONFIG_KEY);

        boolean enabled = policy.isAuditEnabled();

        assertThat(enabled).isEqualTo(expected);
    }

    @Test
    void enablesAuditWhenTheKeyIsAbsent() {
        Optional<AppConfig> result = Optional.empty();
        doReturn(result).when(configuration).findByKey(CONFIG_KEY);

        boolean enabled = policy.isAuditEnabled();

        assertThat(enabled).isTrue();
    }
}
