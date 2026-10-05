package cl.casero.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import cl.casero.migration.domain.MonthlyStatistic;
import cl.casero.migration.service.dto.DashboardData;
import cl.casero.migration.service.dto.OverdueCustomerSummary;
import cl.casero.migration.service.dto.TopCustomerSummary;
import cl.casero.migration.service.dto.TransactionMonthlySummary;
import cl.casero.migration.web.presentation.DashboardChartData;
import cl.casero.migration.web.presentation.DashboardChartPresenter;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final int SALES = 12000;
    private static final int PAYMENTS = 9000;
    private static final long DEBT = 4000L;
    private static final int ITEMS = 8;
    private static final int FINISHED_CARDS = 2;

    @Mock
    private CustomerQueries customers;

    @Mock
    private StatisticsService statistics;

    @Mock
    private TransactionQueries transactions;

    @ParameterizedTest
    @CsvSource({
        "2024-02-29, 2024-01-01, 2024-01-29, 2023-09-01, 2024-02-29, Feb 2024",
        "2025-03-31, 2025-02-01, 2025-02-28, 2024-10-01, 2025-03-31, Mar 2025",
        "2026-01-31, 2025-12-01, 2025-12-31, 2025-08-01, 2026-01-31, Ene 2026"
    })
    void preservesMonthToDateComparisonIndicatorsAndStructuredSeries(
        LocalDate referenceDate, LocalDate previousStart, LocalDate previousEnd,
        LocalDate seriesStart, LocalDate seriesEnd, String expectedLabel
    ) {
        int month = referenceDate.getMonthValue();
        int year = referenceDate.getYear();
        doReturn(1L).when(customers).getBirthdaysThisMonthCount(month);
        doReturn(DEBT).when(customers).count();
        doReturn(DEBT).when(customers).getOverdueDebt(1);
        Pageable pageable = PageRequest.of(0, 1);
        List<OverdueCustomerSummary> noContent = List.of();
        Page<OverdueCustomerSummary> overdue = new PageImpl<>(noContent, pageable, DEBT);
        doReturn(overdue).when(customers).getOverdueCustomers(pageable, 1);
        doReturn(SALES).when(statistics).getTotalDebt();
        doReturn(PAYMENTS).when(statistics).getAverageDebt();
        MonthlyStatistic monthly = new MonthlyStatistic();
        monthly.setSalesCount(SALES);
        monthly.setPaymentsCount(PAYMENTS);
        monthly.setFinishedCardsCount(FINISHED_CARDS);
        monthly.setTotalItemsCount(ITEMS);
        doReturn(monthly).when(statistics).getMonthlyStatistic(month, year);
        doReturn((long) SALES).when(transactions).getSalesSum(previousStart, previousEnd);
        doReturn((long) PAYMENTS).when(transactions).getPaymentsSum(previousStart, previousEnd);
        TopCustomerSummary top = new TopCustomerSummary("Top", PAYMENTS);
        List<TopCustomerSummary> topCustomers = List.of(top);
        doReturn(topCustomers).when(transactions).getTopCustomersThisMonth();
        TransactionMonthlySummary row = new TransactionMonthlySummary(referenceDate, SALES, PAYMENTS);
        List<TransactionMonthlySummary> series = List.of(row);
        doReturn(series).when(transactions).getMonthlySummary(seriesStart, seriesEnd);
        DashboardService service = new DashboardService(customers, statistics, transactions);

        DashboardData result = service.prepare(referenceDate);

        DashboardData expected = new DashboardData(1L, SALES, DEBT, DEBT, DEBT,
                SALES, PAYMENTS, SALES, PAYMENTS, topCustomers, PAYMENTS, FINISHED_CARDS, ITEMS, series);
        assertThat(result).isEqualTo(expected);
        verify(transactions).getSalesSum(previousStart, previousEnd);
        verify(transactions).getPaymentsSum(previousStart, previousEnd);
        verify(transactions).getMonthlySummary(seriesStart, seriesEnd);
        DashboardChartPresenter presenter = new DashboardChartPresenter();
        List<TransactionMonthlySummary> monthlySeries = result.monthlySeries();
        DashboardChartData chart = presenter.present(monthlySeries);
        List<String> labels = chart.labels();
        List<Long> chartSales = chart.sales();
        List<Long> chartPayments = chart.payments();
        assertThat(labels).containsExactly(expectedLabel);
        assertThat(chartSales).containsExactly((long) SALES);
        assertThat(chartPayments).containsExactly((long) PAYMENTS);
    }
}
