package cl.casero.migration.service.dto;

import cl.casero.migration.domain.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserForm {
    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotNull(message = "Debes seleccionar un rol")
    private UserRole role = UserRole.NORMAL;

    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "\\d{4}", message = "El PIN debe tener exactamente 4 dígitos")
    private String pin;
}
