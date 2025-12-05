package cl.casero.migration.api.dto;

import java.time.Instant;
import java.util.Map;

public record AuditEventResponse(
        Long id,
        String eventType,
        UserResponse user,
        Map<String, Object> payload,
        String ip,
        String userAgent,
        Instant createdAt
) {
}
