package cl.casero.migration.api.dto;

public record ConfigResponse(
        Long id,
        String key,
        String value
) {
}
