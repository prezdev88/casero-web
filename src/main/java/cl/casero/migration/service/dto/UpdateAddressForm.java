package cl.casero.migration.service.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateAddressForm {

    @NotBlank
    private String newAddress;

    public UpdateAddressForm() {
    }

    public UpdateAddressForm(String newAddress) {
        this.newAddress = newAddress;
    }

    public String getNewAddress() {
        return newAddress;
    }

    public void setNewAddress(String newAddress) {
        this.newAddress = newAddress;
    }
}
