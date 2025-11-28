package cl.casero.migration.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCustomerForm {
    @NotBlank
    private String name;

    @NotNull
    private Long sectorId;

    @NotBlank
    private String address;
}
