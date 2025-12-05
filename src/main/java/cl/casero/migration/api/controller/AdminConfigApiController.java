package cl.casero.migration.api.controller;

import cl.casero.migration.api.dto.ConfigResponse;
import cl.casero.migration.api.dto.UpdateConfigRequest;
import cl.casero.migration.domain.AppConfig;
import cl.casero.migration.service.AppConfigService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/config")
public class AdminConfigApiController {

    private final AppConfigService appConfigService;

    public AdminConfigApiController(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @GetMapping
    public List<ConfigResponse> listAll() {
        return appConfigService.listAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{key}")
    public ConfigResponse getByKey(@PathVariable String key) {
        return appConfigService.findByKey(key)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Config with key '" + key + "' not found"));
    }

    @PutMapping("/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateConfig(@PathVariable String key,
                             @Valid @RequestBody UpdateConfigRequest request) {
        // Verify config exists
        appConfigService.findByKey(key)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Config with key '" + key + "' not found"));

        appConfigService.updateValue(key, request.value());
    }

    private ConfigResponse toResponse(AppConfig config) {
        return new ConfigResponse(
                config.getId(),
                config.getConfigKey(),
                config.getValue()
        );
    }
}
