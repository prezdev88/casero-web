package cl.casero.migration.service.impl;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.AuditEvent;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.repository.AuditEventRepository;
import cl.casero.migration.service.AppConfigService;
import cl.casero.migration.service.AuditEventService;
import cl.casero.migration.service.dto.AuditContext;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditEventServiceImpl implements AuditEventService {

    private final AuditEventRepository repository;
    private final AppConfigService appConfigService;

    @Override
    @Transactional
    public void logEvent(AuditEventType eventType, Map<String, Object> payload, AuditContext context) {
        if (!appConfigService.isAuditEnabled()) {
            return;
        }

        if (eventType == null) {
            return;
        }

        AppUser user = context.user();
        if (user != null && user.isAdmin()) {
            return;
        }

        persistEvent(eventType, payload, context);
    }

    private void persistEvent(AuditEventType eventType, Map<String, Object> payload, AuditContext context) {
        try {
            AppUser user = context.user();
            String ip = context.ip();
            String userAgent = context.userAgent();
            Map<String, Object> eventPayload = (payload != null) ? new HashMap<>(payload) : Map.of();
            AuditEvent event = new AuditEvent();
            event.setEventType(eventType);
            event.setUser(user);
            event.setPayload(eventPayload);
            event.setIp(ip);
            event.setUserAgent(userAgent);
            repository.save(event);
        } catch (Exception ex) {
            String reason = ex.getMessage();
            log.warn("No se pudo registrar evento de auditoría {}: {}", eventType, reason);
        }
    }
}
