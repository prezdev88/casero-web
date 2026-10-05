package cl.casero.migration.service.dto;

import cl.casero.migration.domain.AppUser;

/**
 * Actor and source metadata prepared before recording an event.
 * A null user denotes an anonymous actor; null IP or user agent denotes absent metadata.
 */
public record AuditContext(AppUser user, String ip, String userAgent) {}
