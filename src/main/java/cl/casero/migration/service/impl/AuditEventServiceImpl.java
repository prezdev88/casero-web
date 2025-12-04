package cl.casero.migration.service.impl;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.AuditEvent;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.repository.AuditEventRepository;
import cl.casero.migration.service.AppConfigService;
import cl.casero.migration.service.AuditEventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@AllArgsConstructor
@Slf4j
public class AuditEventServiceImpl implements AuditEventService {

    private final AuditEventRepository repository;
    private final AppConfigService appConfigService;

    @Override
    @Transactional
    public void logEvent(AuditEventType eventType, AppUser user, String payload, HttpServletRequest request) {
        if (!appConfigService.isAuditEnabled()) {
            return;
        }
        if (eventType == null) {
            return;
        }
        try {
            AuditEvent event = new AuditEvent();
            event.setEventType(eventType);
            event.setUser(user);
            event.setPayload(payload != null ? payload : "");
            if (request != null) {
                event.setIp(resolveIp(request));
                event.setUserAgent(request.getHeader("User-Agent"));
            }
            repository.save(event);
        } catch (Exception ex) {
            log.warn("No se pudo registrar evento de auditoría {}: {}", eventType, ex.getMessage());
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
