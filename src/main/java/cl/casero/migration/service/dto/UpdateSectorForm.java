package cl.casero.migration.service.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateSectorForm {

    @NotNull(message = "Selecciona un sector")
    private Long sectorId;

    public Long getSectorId() {
        return sectorId;
    }

    public void setSectorId(Long sectorId) {
        this.sectorId = sectorId;
    }
}
