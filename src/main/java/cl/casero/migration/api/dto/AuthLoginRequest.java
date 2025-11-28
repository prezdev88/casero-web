package cl.casero.migration.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AuthLoginRequest(
        @NotBlank(message = "El PIN es obligatorio")
        @Pattern(regexp = "\\d{4,12}", message = "El PIN debe tener entre 4 y 12 dígitos")
        String pin) {
}
