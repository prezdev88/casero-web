package cl.casero.migration.service.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateNameForm {

    @NotBlank
    private String newName;

    public UpdateNameForm() {
    }

    public UpdateNameForm(String newName) {
        this.newName = newName;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }
}
