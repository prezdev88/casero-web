package cl.casero.migration.service.dto;

/**
 * Actor and source metadata prepared before recording an event.
 * A null user denotes an anonymous actor; null IP or user agent denotes absent metadata.
 */
public record AuditContext(UserIdentity user, String ip, String userAgent) {}
