package cl.casero.migration.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.casero.migration.domain.AuditEvent;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.repository.AuditEventRepository;
import cl.casero.migration.service.AuditQueries;
import cl.casero.migration.service.dto.AuditSearchCriteria;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditQueryService implements AuditQueries {

    private final AuditEventRepository repository;

    @Override
    public Page<AuditEvent> search(AuditSearchCriteria criteria) {
        AuditEventType eventType = criteria.eventType();
        String payloadType = criteria.payloadType();
        Pageable pageable = criteria.pageable();
        if (payloadType != null && eventType != null) {
            return repository.findByEventTypeAndPayloadType(eventType, payloadType, pageable);
        }

        if (payloadType != null) {
            return repository.findByPayloadType(payloadType, pageable);
        }

        if (eventType != null) {
            return repository.findByEventTypeOrderByCreatedAtDesc(eventType, pageable);
        }

        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
