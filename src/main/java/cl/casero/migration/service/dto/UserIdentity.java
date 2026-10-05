package cl.casero.migration.service.dto;

import java.io.Serializable;

import cl.casero.migration.domain.enums.UserRole;

public record UserIdentity(Long id, String name, UserRole role, boolean enabled) implements Serializable {

    private static final long serialVersionUID = 1L;

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
