package cl.casero.migration.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSectorForm {
    @NotNull(message = "Selecciona un sector")
    private Long sectorId;
}
