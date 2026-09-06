package cl.casero.migration.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBirthdateForm {
    
    @NotNull(message = "El día es obligatorio")
    @Min(value = 1, message = "Día inválido")
    @Max(value = 31, message = "Día inválido")
    private Integer day;

    @NotNull(message = "El mes es obligatorio")
    @Min(value = 1, message = "Mes inválido")
    @Max(value = 12, message = "Mes inválido")
    private Integer month;

    @Min(value = 1900, message = "Año muy antiguo")
    private Integer year;
}
