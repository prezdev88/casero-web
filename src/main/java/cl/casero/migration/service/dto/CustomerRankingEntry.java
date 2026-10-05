package cl.casero.migration.service.dto;

public record CustomerRankingEntry(
    Long id,
    String name,
    Integer debt,
    Double score,
    String explanation,
    int cycleCount
) {}
