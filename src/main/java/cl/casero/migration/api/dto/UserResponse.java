package cl.casero.migration.api.dto;

import cl.casero.migration.domain.enums.UserRole;
import java.time.Instant;

public record UserResponse(Long id,
                           String name,
                           UserRole role,
                           boolean enabled,
                           Instant createdAt,
                           Instant updatedAt) {
}
