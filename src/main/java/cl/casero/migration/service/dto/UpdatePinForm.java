package cl.casero.migration.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class UpdatePinForm {

    @NotNull
    private Long userId;

    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "\\d{4}", message = "El PIN debe tener exactamente 4 dígitos")
    private String pin;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
