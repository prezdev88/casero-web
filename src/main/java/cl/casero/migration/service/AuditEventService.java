package cl.casero.migration.service;

import java.util.Map;

import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.service.dto.AuditContext;

public interface AuditEventService {

    /** Context is required; a null payload is recorded as an empty map. */
    void logEvent(AuditEventType eventType, Map<String, Object> payload, AuditContext context);
}
