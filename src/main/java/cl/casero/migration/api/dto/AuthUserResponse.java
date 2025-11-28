package cl.casero.migration.api.dto;

import cl.casero.migration.domain.enums.UserRole;

public record AuthUserResponse(Long id,
                               String name,
                               UserRole role,
                               boolean authenticated) {
}
