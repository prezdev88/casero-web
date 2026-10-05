package cl.casero.migration.service;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import cl.casero.migration.domain.MonthlyStatistic;
import cl.casero.migration.service.dto.DashboardData;
import cl.casero.migration.service.dto.OverdueCustomerSummary;
import cl.casero.migration.service.dto.TopCustomerSummary;
import cl.casero.migration.service.dto.TransactionMonthlySummary;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int OVERDUE_MONTHS = 1;
    private static final int CHART_MONTHS = 6;

    private final CustomerQueries customerQueries;
    private final StatisticsService statisticsService;
    private final TransactionQueries transactionQueries;

    public DashboardData prepare(LocalDate referenceDate) {
        int month = referenceDate.getMonthValue();
        int year = referenceDate.getYear();
        long birthdaysCount = customerQueries.getBirthdaysThisMonthCount(month);
        int totalDebt = statisticsService.getTotalDebt();
        long activeCustomersCount = customerQueries.count();
        Pageable overduePageable = PageRequest.of(0, 1);
        Page<OverdueCustomerSummary> overdueCustomers = customerQueries.getOverdueCustomers(overduePageable, OVERDUE_MONTHS);
        long overdueCustomersCount = overdueCustomers.getTotalElements();
        long overdueDebt = customerQueries.getOverdueDebt(OVERDUE_MONTHS);
        MonthlyStatistic statistic = statisticsService.getMonthlyStatistic(month, year);
        long salesAmount = statistic.getSalesCount();
        long paymentsAmount = statistic.getPaymentsCount();
        LocalDate previousMonthDate = referenceDate.minusMonths(1);
        LocalDate previousMonthStart = previousMonthDate.withDayOfMonth(1);
        long lastMonthSales = transactionQueries.getSalesSum(previousMonthStart, previousMonthDate);
        long lastMonthPayments = transactionQueries.getPaymentsSum(previousMonthStart, previousMonthDate);
        List<TopCustomerSummary> topCustomers = transactionQueries.getTopCustomersThisMonth();
        int averageDebt = statisticsService.getAverageDebt();
        int finishedCardsCount = statistic.getFinishedCardsCount();
        int totalItemsCount = statistic.getTotalItemsCount();
        LocalDate firstChartMonth = referenceDate.minusMonths(CHART_MONTHS - 1);
        LocalDate seriesStart = firstChartMonth.withDayOfMonth(1);
        int finalDay = referenceDate.lengthOfMonth();
        LocalDate seriesEnd = referenceDate.withDayOfMonth(finalDay);
        List<TransactionMonthlySummary> monthlySeries = transactionQueries.getMonthlySummary(seriesStart, seriesEnd);
        return new DashboardData(birthdaysCount, totalDebt, activeCustomersCount, overdueCustomersCount,
                overdueDebt, salesAmount, paymentsAmount, lastMonthSales, lastMonthPayments,
                topCustomers, averageDebt, finishedCardsCount, totalItemsCount, monthlySeries);
    }
}
