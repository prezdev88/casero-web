package cl.casero.migration.service.dto;

import cl.casero.migration.domain.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CreateUserForm {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotNull(message = "Debes seleccionar un rol")
    private UserRole role = UserRole.NORMAL;

    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "\\d{4}", message = "El PIN debe tener exactamente 4 dígitos")
    private String pin;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
