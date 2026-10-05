package cl.casero.migration.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.repository.TransactionRepository;
import cl.casero.migration.service.TransactionQueries;
import cl.casero.migration.service.dto.TransactionMonthlySummary;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceTest {

    private static final long CUSTOMER_ID = 7L;
    private static final int SALE_AMOUNT = 100;
    private static final int PAYMENT_AMOUNT = 40;
    private static final int REFUND_AMOUNT = 20;
    private static final int DEFAULT_MONTH_COUNT = 6;
    private static final Instant REFERENCE_INSTANT = Instant.parse("2024-02-15T12:00:00Z");
    private static final LocalDate DEFAULT_START = LocalDate.parse("2023-09-01");
    private static final LocalDate CURRENT_START = LocalDate.parse("2024-02-01");
    private static final LocalDate CURRENT_END = LocalDate.parse("2024-02-29");

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionQueryService queries;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(REFERENCE_INSTANT, ZoneOffset.UTC);
        queries = new TransactionQueryService(transactionRepository, clock);
    }

    @ParameterizedTest
    @CsvSource({"2023-12-15, 2024-02-05", "2024-02-05, 2023-12-15"})
    void preservesInclusiveMonthsReversedRangesAndZeroFilledTotals(String rawStart, String rawEnd) {
        LocalDate start = LocalDate.parse(rawStart);
        LocalDate end = LocalDate.parse(rawEnd);
        LocalDate normalizedStart = LocalDate.parse("2023-12-01");
        LocalDate decemberDate = LocalDate.parse("2023-12-20");
        Transaction payment = transaction(CURRENT_START, TransactionType.PAYMENT, PAYMENT_AMOUNT);
        Transaction sale = transaction(decemberDate, TransactionType.SALE, SALE_AMOUNT);
        Transaction refund = transaction(decemberDate, TransactionType.REFUND, REFUND_AMOUNT);
        List<Transaction> transactions = List.of(payment, refund, sale);
        doReturn(transactions).when(transactionRepository).findVisibleByDateBetween(normalizedStart, CURRENT_END);

        List<TransactionMonthlySummary> result = queries.getMonthlySummary(start, end);

        TransactionMonthlySummary december = new TransactionMonthlySummary(normalizedStart, SALE_AMOUNT, 0);
        LocalDate januaryDate = LocalDate.parse("2024-01-01");
        TransactionMonthlySummary january = new TransactionMonthlySummary(januaryDate, 0, 0);
        TransactionMonthlySummary february = new TransactionMonthlySummary(CURRENT_START, 0, PAYMENT_AMOUNT);
        assertThat(result).containsExactly(december, january, february);
    }

    @Test
    void defaultsToSixCalendarMonthsIncludingTheCurrentLeapMonth() {
        List<Transaction> transactions = List.of();
        doReturn(transactions).when(transactionRepository).findVisibleByDateBetween(DEFAULT_START, CURRENT_END);

        List<TransactionMonthlySummary> result = queries.getMonthlySummary(null, null);

        assertThat(result).hasSize(DEFAULT_MONTH_COUNT);
        TransactionMonthlySummary first = result.get(0);
        TransactionMonthlySummary last = result.get(DEFAULT_MONTH_COUNT - 1);
        LocalDate firstDate = first.month();
        LocalDate lastDate = last.month();
        assertThat(firstDate).isEqualTo(DEFAULT_START);
        assertThat(lastDate).isEqualTo(CURRENT_START);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {100})
    void preservesZeroForAbsentSumsAndReturnsExistingAmounts(Integer amount) {
        doReturn(amount).when(transactionRepository).sumByTypeAndDateRange(TransactionType.SALE, CURRENT_START, CURRENT_END);
        doReturn(amount).when(transactionRepository).sumByTypeAndDateRange(TransactionType.PAYMENT, CURRENT_START, CURRENT_END);

        long sales = queries.getSalesSum(CURRENT_START, CURRENT_END);
        long payments = queries.getPaymentsSum(CURRENT_START, CURRENT_END);

        long expected = (amount == null) ? 0 : amount;
        assertThat(sales).isEqualTo(expected);
        assertThat(payments).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 2, 5})
    void keepsRepositoryOrderAndExistingLimitMeaning(int limit) {
        Transaction first = transaction(CURRENT_END, TransactionType.PAYMENT, PAYMENT_AMOUNT);
        Transaction second = transaction(CURRENT_START, TransactionType.SALE, SALE_AMOUNT);
        List<Transaction> transactions = List.of(first, second);
        doReturn(transactions).when(transactionRepository).findVisibleByCustomerIdOrderByDateDescIdDesc(CUSTOMER_ID);

        List<Transaction> result = queries.listRecentByCustomer(CUSTOMER_ID, limit);

        if (limit == 1) {
            assertThat(result).containsExactly(first);
        } else {
            assertThat(result).isSameAs(transactions).containsExactly(first, second);
        }
    }

    @Test
    void executesMonthlyQueriesInsideAReadOnlyTransaction() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            RecordingTransactionManager manager = new RecordingTransactionManager();
            context.register(TransactionTestConfiguration.class);
            context.registerBean(TransactionRepository.class, () -> transactionRepository);
            context.registerBean(Clock.class, () -> clock);
            context.registerBean(PlatformTransactionManager.class, () -> manager);
            context.registerBean(TransactionQueryService.class);
            context.refresh();
            List<Transaction> transactions = List.of();
            Answer<List<Transaction>> answer = invocation -> {
                boolean active = TransactionSynchronizationManager.isActualTransactionActive();
                boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
                assertThat(active).isTrue();
                assertThat(readOnly).isTrue();
                return transactions;
            };
            doAnswer(answer).when(transactionRepository).findSalesThisMonth(CURRENT_START, CURRENT_END);
            TransactionQueries transactionalQueries = context.getBean(TransactionQueries.class);

            List<Transaction> result = transactionalQueries.getSalesThisMonth();

            int begins = manager.getBeginCount();
            int commits = manager.getCommitCount();
            boolean readOnly = manager.isReadOnly();
            assertThat(result).isEmpty();
            assertThat(begins).isEqualTo(1);
            assertThat(commits).isEqualTo(1);
            assertThat(readOnly).isTrue();
        }
    }

    private Transaction transaction(LocalDate date, TransactionType type, int amount) {
        Transaction transaction = new Transaction();
        transaction.setDate(date);
        transaction.setType(type);
        transaction.setAmount(amount);
        return transaction;
    }
}
