package cl.casero.migration.api.dto;

public record OverdueCustomerResponse(Long id,
                                      String name,
                                      String sector,
                                      Integer debt,
                                      String lastPayment,
                                      Integer monthsOverdue) {
}
