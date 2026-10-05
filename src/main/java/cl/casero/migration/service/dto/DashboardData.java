package cl.casero.migration.service.dto;

import java.util.List;

public record DashboardData(
    long birthdaysCount,
    int totalDebt,
    long activeCustomersCount,
    long overdueCustomersCount,
    long overdueDebt,
    long salesAmount,
    long paymentsAmount,
    long lastMonthSales,
    long lastMonthPayments,
    List<TopCustomerSummary> topCustomers,
    int averageDebt,
    int finishedCardsCount,
    int totalItemsCount,
    List<TransactionMonthlySummary> monthlySeries
) {
    public DashboardData {
        topCustomers = List.copyOf(topCustomers);
        monthlySeries = List.copyOf(monthlySeries);
    }
}
