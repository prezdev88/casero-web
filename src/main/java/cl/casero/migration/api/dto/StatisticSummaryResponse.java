package cl.casero.migration.api.dto;

import java.util.List;

public record StatisticSummaryResponse(int totalDebt,
                                       int averageDebt,
                                       long customersCount,
                                       List<CustomerSummaryResponse> topDebtors,
                                       List<CustomerSummaryResponse> bestCustomers) {
}
