package cl.casero.migration.service.dto;

import org.springframework.data.domain.Pageable;

import cl.casero.migration.domain.enums.AuditEventType;

public record AuditSearchCriteria(AuditEventType eventType, String payloadType, Pageable pageable) {}
