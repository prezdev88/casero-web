package cl.casero.migration.service;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.AuditEventType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface AuditEventService {
    void logEvent(AuditEventType eventType, AppUser user, Map<String, Object> payload, HttpServletRequest request);
}
