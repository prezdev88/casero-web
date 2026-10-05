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
import cl.casero.migration.repository.AppUserRepository;
import cl.casero.migration.repository.AuditEventRepository;
import cl.casero.migration.service.AuditEventService;
import cl.casero.migration.service.AuditPolicy;
import cl.casero.migration.service.dto.AuditContext;
import cl.casero.migration.service.dto.UserIdentity;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditEventServiceImpl implements AuditEventService {

    private final AuditEventRepository repository;
    private final AuditPolicy auditPolicy;
    private final AppUserRepository users;

    @Override
    @Transactional
    public void logEvent(AuditEventType eventType, Map<String, Object> payload, AuditContext context) {
        if (!auditPolicy.isAuditEnabled()) {
            return;
        }

        if (eventType == null) {
            return;
        }

        UserIdentity user = context.user();
        if (user != null && user.isAdmin()) {
            return;
        }

        persistEvent(eventType, payload, context);
    }

    private void persistEvent(AuditEventType eventType, Map<String, Object> payload, AuditContext context) {
        try {
            UserIdentity identity = context.user();
            AppUser user = resolveActor(identity);
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

    private AppUser resolveActor(UserIdentity identity) {
        if (identity == null) {
            return null;
        }

        Long id = identity.id();
        return users.getReferenceById(id);
    }
}
