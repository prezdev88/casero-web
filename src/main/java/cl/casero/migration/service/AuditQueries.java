package cl.casero.migration.service;

import org.springframework.data.domain.Page;

import cl.casero.migration.domain.AuditEvent;
import cl.casero.migration.service.dto.AuditSearchCriteria;

public interface AuditQueries {
    Page<AuditEvent> search(AuditSearchCriteria criteria);
}
