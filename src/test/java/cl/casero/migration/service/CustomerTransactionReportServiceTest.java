package cl.casero.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.dto.CustomerTransactionReportData;
import cl.casero.migration.service.dto.TransactionReportCriteria;
import cl.casero.migration.service.dto.TransactionReportCriteria.ReportRange;

@ExtendWith(MockitoExtension.class)
class CustomerTransactionReportServiceTest {

    private static final long CUSTOMER_ID = 7L;
    private static final int SINGLE_MONTH = 1;
    private static final Instant REPORT_INSTANT = Instant.parse("2026-10-04T12:00:00Z");
    private static final LocalDate DEFAULT_CUTOFF = LocalDate.parse("2025-11-01");
    private static final LocalDate FUTURE_DATE = LocalDate.parse("2027-01-01");

    @Mock
    private CustomerService customerService;

    @Mock
    private TransactionQueries transactionQueries;

    private CustomerTransactionReportService reportService;
    private Customer customer;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(REPORT_INSTANT, ZoneOffset.UTC);
        reportService = new CustomerTransactionReportService(customerService, transactionQueries, clock);
        customer = new Customer();
        customer.setId(CUSTOMER_ID);
    }

    @ParameterizedTest
    @CsvSource({
        "-1, 2025-11-01, Últimos 12 meses",
        "0, 2025-11-01, Últimos 12 meses",
        "1, 2026-10-01, Últimos 1 mes",
        "2, 2026-09-01, Últimos 2 meses",
        "12, 2025-11-01, Últimos 12 meses",
        "60, 2021-11-01, Últimos 60 meses",
        "61, 2021-11-01, Últimos 60 meses",
        "120, 2021-11-01, Últimos 60 meses"
    })
    void preservesCalendarBoundariesDefaultsAndMaximum(
        Integer requestedMonths,
        String cutoffDate,
        String expectedLabel
    ) {
        LocalDate cutoff = LocalDate.parse(cutoffDate);
        LocalDate previousDay = cutoff.minusDays(1);
        Transaction beforeBoundary = transaction(previousDay, TransactionType.PAYMENT);
        Transaction onBoundary = transaction(cutoff, TransactionType.PAYMENT);
        Transaction undated = transaction(null, TransactionType.PAYMENT);
        Transaction future = transaction(FUTURE_DATE, TransactionType.PAYMENT);
        List<Transaction> transactions = List.of(beforeBoundary, onBoundary, undated, future);
        TransactionReportCriteria criteria = new TransactionReportCriteria(ReportRange.MONTHS, requestedMonths, null);

        CustomerTransactionReportData report = prepare(criteria, transactions);

        List<Transaction> selected = report.transactions();
        String label = report.rangeLabel();
        Customer reportCustomer = report.customer();
        assertThat(selected).containsExactly(onBoundary, future);
        assertThat(label).isEqualTo(expectedLabel);
        assertThat(reportCustomer).isSameAs(customer);
        assertThat(transactions).containsExactly(beforeBoundary, onBoundary, undated, future);
    }

    @Test
    void usesTwelveMonthsWhenTheCountIsMissing() {
        LocalDate previousDay = DEFAULT_CUTOFF.minusDays(1);
        Transaction beforeBoundary = transaction(previousDay, TransactionType.SALE);
        Transaction onBoundary = transaction(DEFAULT_CUTOFF, TransactionType.SALE);
        List<Transaction> transactions = List.of(beforeBoundary, onBoundary);
        TransactionReportCriteria criteria = new TransactionReportCriteria(ReportRange.MONTHS, null, null);

        CustomerTransactionReportData report = prepare(criteria, transactions);

        List<Transaction> selected = report.transactions();
        String label = report.rangeLabel();
        assertThat(selected).containsExactly(onBoundary);
        assertThat(label).isEqualTo("Últimos 12 meses");
    }

    @Test
    void includesAllDatesAndCategoriesInOriginalOrderForAllRange() {
        Transaction undated = transaction(null, TransactionType.SALE);
        Transaction oldPayment = transaction(DEFAULT_CUTOFF, TransactionType.PAYMENT);
        Transaction future = transaction(FUTURE_DATE, TransactionType.REFUND);
        List<Transaction> transactions = List.of(undated, oldPayment, future);
        TransactionReportCriteria criteria = new TransactionReportCriteria(ReportRange.ALL, SINGLE_MONTH, null);

        CustomerTransactionReportData report = prepare(criteria, transactions);

        List<Transaction> selected = report.transactions();
        String label = report.rangeLabel();
        TransactionType filterType = report.filterType();
        assertThat(selected).containsExactly(undated, oldPayment, future);
        assertThat(label).isEqualTo("Todas las transacciones");
        assertThat(filterType).isNull();
    }

    @Test
    void appliesTypeAndCalendarSelectionTogether() {
        LocalDate cutoff = LocalDate.parse("2026-10-01");
        LocalDate previousDay = cutoff.minusDays(1);
        Transaction oldPayment = transaction(previousDay, TransactionType.PAYMENT);
        Transaction currentSale = transaction(cutoff, TransactionType.SALE);
        Transaction currentPayment = transaction(cutoff, TransactionType.PAYMENT);
        Transaction futurePayment = transaction(FUTURE_DATE, TransactionType.PAYMENT);
        List<Transaction> transactions = List.of(oldPayment, currentSale, currentPayment, futurePayment);
        TransactionReportCriteria criteria = new TransactionReportCriteria(
                ReportRange.MONTHS, SINGLE_MONTH, TransactionType.PAYMENT);

        CustomerTransactionReportData report = prepare(criteria, transactions);

        List<Transaction> selected = report.transactions();
        TransactionType filterType = report.filterType();
        assertThat(selected).containsExactly(currentPayment, futurePayment);
        assertThat(filterType).isEqualTo(TransactionType.PAYMENT);
    }

    @Test
    void keepsUndatedMatchingTransactionsWhenOnlyTypeIsSelected() {
        Transaction undatedPayment = transaction(null, TransactionType.PAYMENT);
        Transaction sale = transaction(DEFAULT_CUTOFF, TransactionType.SALE);
        Transaction oldPayment = transaction(DEFAULT_CUTOFF, TransactionType.PAYMENT);
        List<Transaction> transactions = List.of(undatedPayment, sale, oldPayment);
        TransactionReportCriteria criteria = new TransactionReportCriteria(
                ReportRange.ALL, SINGLE_MONTH, TransactionType.PAYMENT);

        CustomerTransactionReportData report = prepare(criteria, transactions);

        List<Transaction> selected = report.transactions();
        assertThat(selected).containsExactly(undatedPayment, oldPayment);
    }

    @ParameterizedTest
    @EnumSource(ReportRange.class)
    void preparesEmptyReports(ReportRange range) {
        List<Transaction> transactions = List.of();
        TransactionReportCriteria criteria = new TransactionReportCriteria(range, SINGLE_MONTH, TransactionType.PAYMENT);

        CustomerTransactionReportData report = prepare(criteria, transactions);

        List<Transaction> selected = report.transactions();
        assertThat(selected).isEmpty();
    }

    @Test
    void preservesMissingCustomerFailure() {
        CustomerNotFoundException failure = new CustomerNotFoundException(CUSTOMER_ID);
        doThrow(failure).when(customerService).get(CUSTOMER_ID);
        TransactionReportCriteria criteria = new TransactionReportCriteria(ReportRange.ALL, null, null);

        assertThatThrownBy(() -> reportService.prepare(CUSTOMER_ID, criteria)).isSameAs(failure);
        verifyNoInteractions(transactionQueries);
    }

    private CustomerTransactionReportData prepare(TransactionReportCriteria criteria, List<Transaction> transactions) {
        doReturn(customer).when(customerService).get(CUSTOMER_ID);
        doReturn(transactions).when(transactionQueries).listAllByCustomer(CUSTOMER_ID);
        return reportService.prepare(CUSTOMER_ID, criteria);
    }

    private Transaction transaction(LocalDate date, TransactionType type) {
        Transaction transaction = new Transaction();
        transaction.setDate(date);
        transaction.setType(type);
        return transaction;
    }
}
