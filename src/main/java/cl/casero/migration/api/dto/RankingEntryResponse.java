package cl.casero.migration.api.dto;

public record RankingEntryResponse(Long id,
                                   String name,
                                   Integer debt,
                                   Double score,
                                   String explanation,
                                   int cycleCount) {
}
