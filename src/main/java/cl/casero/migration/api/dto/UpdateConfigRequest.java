package cl.casero.migration.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateConfigRequest(
        @NotBlank(message = "Value is required")
        String value
) {
}
