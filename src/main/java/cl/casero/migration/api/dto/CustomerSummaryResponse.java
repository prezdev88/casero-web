package cl.casero.migration.api.dto;

public record CustomerSummaryResponse(Long id,
                                      String name,
                                      String address,
                                      Long sectorId,
                                      String sectorName,
                                      Integer debt,
                                      Double score) {
}
