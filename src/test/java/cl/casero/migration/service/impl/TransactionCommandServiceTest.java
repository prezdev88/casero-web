package cl.casero.migration.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Statistic;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.SaleType;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.repository.StatisticRepository;
import cl.casero.migration.repository.TransactionRepository;
import cl.casero.migration.service.TransactionCommands;
import cl.casero.migration.service.command.DebtForgivenessCommand;
import cl.casero.migration.service.command.MoneyTransactionCommand;
import cl.casero.migration.service.command.PaymentCommand;
import cl.casero.migration.service.command.SaleCommand;

@ExtendWith(MockitoExtension.class)
class TransactionCommandServiceTest {

    private static final long CUSTOMER_ID = 7L;
    private static final long TRANSACTION_ID = 11L;
    private static final int SALE_AMOUNT = 100;
    private static final int ITEMS_COUNT = 3;
    private static final int INITIAL_DEBT = 200;
    private static final int RESTORED_DEBT = 80;
    private static final LocalDate MOVEMENT_DATE = LocalDate.parse("2026-10-04");

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private StatisticRepository statisticRepository;

    private AnnotationConfigApplicationContext context;
    private RecordingTransactionManager transactionManager;
    private TransactionCommands commands;
    private Customer customer;
    private List<Boolean> activeWrites;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(CUSTOMER_ID);
        activeWrites = new ArrayList<>();
        transactionManager = new RecordingTransactionManager();
        context = new AnnotationConfigApplicationContext();
        context.register(TransactionTestConfiguration.class);
        context.registerBean(CustomerRepository.class, () -> customerRepository);
        context.registerBean(TransactionRepository.class, () -> transactionRepository);
        context.registerBean(StatisticRepository.class, () -> statisticRepository);
        context.registerBean(PlatformTransactionManager.class, () -> transactionManager);
        context.registerBean(TransactionCommandService.class);
        context.refresh();
        commands = context.getBean(TransactionCommands.class);
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @ParameterizedTest
    @CsvSource({"0, 100, NEW_SALE", "200, 300, MAINTENANCE"})
    void keepsSaleBalanceItemsAndClassification(int initialDebt, int expectedBalance, SaleType expectedSaleType) {
        prepareCustomer(initialDebt);
        trackSuccessfulWrites();
        SaleCommand command = saleCommand();

        commands.registerSale(CUSTOMER_ID, command);

        PersistedMovement movement = captureWrites();
        assertMovement(movement, TransactionType.SALE, SALE_AMOUNT, expectedBalance);
        Transaction transaction = movement.transaction();
        Statistic statistic = movement.statistic();
        Integer transactionItems = transaction.getItemCount();
        Integer statisticItems = statistic.getItemsCount();
        SaleType saleType = statistic.getSaleType();
        assertThat(transactionItems).isEqualTo(ITEMS_COUNT);
        assertThat(statisticItems).isEqualTo(ITEMS_COUNT);
        assertThat(saleType).isEqualTo(expectedSaleType);
        assertCommittedWriteTransaction();
    }

    @ParameterizedTest
    @CsvSource({"100, 100", "300, 0"})
    void keepsPaymentAmountWhileClampingDebt(int amount, int expectedBalance) {
        prepareCustomer(INITIAL_DEBT);
        trackSuccessfulWrites();
        PaymentCommand command = new PaymentCommand(MOVEMENT_DATE, amount);

        commands.registerPayment(CUSTOMER_ID, command);

        PersistedMovement movement = captureWrites();
        assertMovement(movement, TransactionType.PAYMENT, amount, expectedBalance);
        Transaction transaction = movement.transaction();
        Statistic statistic = movement.statistic();
        String detail = transaction.getDetail();
        Integer itemsCount = statistic.getItemsCount();
        SaleType saleType = statistic.getSaleType();
        String expectedDetail = "[Abono]: $" + amount;
        assertThat(detail).isEqualTo(expectedDetail);
        assertThat(itemsCount).isNull();
        assertThat(saleType).isNull();
        assertCommittedWriteTransaction();
    }

    @ParameterizedTest
    @CsvSource({"REFUND, 100, 100", "REFUND, 300, 0", "FAULT_DISCOUNT, 100, 100", "FAULT_DISCOUNT, 300, 0"})
    void keepsRefundAndFaultDiscountBehavior(TransactionType type, int amount, int expectedBalance) {
        prepareCustomer(INITIAL_DEBT);
        trackSuccessfulWrites();
        MoneyTransactionCommand command = new MoneyTransactionCommand(MOVEMENT_DATE, "Test adjustment", amount);

        if (type == TransactionType.REFUND) {
            commands.registerRefund(CUSTOMER_ID, command);
        } else {
            commands.registerFaultDiscount(CUSTOMER_ID, command);
        }

        PersistedMovement movement = captureWrites();
        assertMovement(movement, type, amount, expectedBalance);
        Transaction transaction = movement.transaction();
        String detail = transaction.getDetail();
        assertThat(detail).isEqualTo("Test adjustment");
        assertCommittedWriteTransaction();
    }

    @Test
    void forgivesExactlyTheOutstandingDebt() {
        prepareCustomer(INITIAL_DEBT);
        trackSuccessfulWrites();
        DebtForgivenessCommand command = forgivenessCommand();

        commands.forgiveDebt(CUSTOMER_ID, command);

        PersistedMovement movement = captureWrites();
        assertMovement(movement, TransactionType.DEBT_FORGIVENESS, INITIAL_DEBT, 0);
        assertCommittedWriteTransaction();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void doesNotPersistForgivenessWhenDebtIsNotPositive(int debt) {
        prepareCustomer(debt);
        DebtForgivenessCommand command = forgivenessCommand();

        commands.forgiveDebt(CUSTOMER_ID, command);

        verifyNoInteractions(transactionRepository, statisticRepository);
        Integer remainingDebt = customer.getDebt();
        assertThat(remainingDebt).isEqualTo(debt);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deletesStatisticsAndMovementBeforeRestoringTheLatestBalance(boolean hasRemainingMovement) {
        Transaction removed = new Transaction();
        removed.setCustomer(customer);
        removed.setType(TransactionType.PAYMENT);
        removed.setAmount(SALE_AMOUNT);
        removed.setDate(MOVEMENT_DATE);
        Optional<Transaction> existing = Optional.of(removed);
        doReturn(existing).when(transactionRepository).findById(TRANSACTION_ID);
        Transaction latest = new Transaction();
        latest.setBalance(RESTORED_DEBT);
        List<Transaction> remaining = hasRemainingMovement ? List.of(latest) : List.of();
        Page<Transaction> latestPage = new PageImpl<>(remaining);
        Pageable limit = Pageable.ofSize(1);
        doReturn(latestPage).when(transactionRepository).findLatestVisibleByCustomerId(CUSTOMER_ID, limit);

        commands.delete(TRANSACTION_ID);

        InOrder order = inOrder(statisticRepository, transactionRepository, customerRepository);
        order.verify(statisticRepository).deleteByTypeAndAmountAndDate(TransactionType.PAYMENT, SALE_AMOUNT, MOVEMENT_DATE);
        order.verify(transactionRepository).delete(removed);
        order.verify(transactionRepository).findLatestVisibleByCustomerId(CUSTOMER_ID, limit);
        order.verify(customerRepository).save(customer);
        Integer debt = customer.getDebt();
        int expectedDebt = hasRemainingMovement ? RESTORED_DEBT : 0;
        int begins = transactionManager.getBeginCount();
        int commits = transactionManager.getCommitCount();
        assertThat(debt).isEqualTo(expectedDebt);
        assertThat(begins).isEqualTo(1);
        assertThat(commits).isEqualTo(1);
    }

    @Test
    void requestsRollbackWhenTheFinalStatisticsWriteFails() {
        prepareCustomer(INITIAL_DEBT);
        trackCustomerAndTransactionWrites();
        IllegalStateException failure = new IllegalStateException("Statistics persistence failed");
        StatisticRepository stub = doAnswer(invocation -> {
            boolean active = TransactionSynchronizationManager.isActualTransactionActive();
            activeWrites.add(active);
            throw failure;
        }).when(statisticRepository);
        Statistic matchedStatistic = any(Statistic.class);
        stub.save(matchedStatistic);
        SaleCommand command = saleCommand();

        assertThatThrownBy(() -> commands.registerSale(CUSTOMER_ID, command)).isSameAs(failure);

        int begins = transactionManager.getBeginCount();
        int commits = transactionManager.getCommitCount();
        int rollbacks = transactionManager.getRollbackCount();
        assertThat(activeWrites).containsExactly(true, true, true);
        assertThat(begins).isEqualTo(1);
        assertThat(commits).isZero();
        assertThat(rollbacks).isEqualTo(1);
    }

    private void prepareCustomer(int debt) {
        customer.setDebt(debt);
        Optional<Customer> existing = Optional.of(customer);
        doReturn(existing).when(customerRepository).findByIdAndEnabledTrue(CUSTOMER_ID);
    }

    private void trackSuccessfulWrites() {
        trackCustomerAndTransactionWrites();
        Answer<Object> answer = this::recordWrite;
        StatisticRepository stub = doAnswer(answer).when(statisticRepository);
        Statistic statistic = any(Statistic.class);
        stub.save(statistic);
    }

    private void trackCustomerAndTransactionWrites() {
        Answer<Object> answer = this::recordWrite;
        CustomerRepository customerStub = doAnswer(answer).when(customerRepository);
        Customer matchedCustomer = any(Customer.class);
        customerStub.save(matchedCustomer);
        TransactionRepository transactionStub = doAnswer(answer).when(transactionRepository);
        Transaction matchedTransaction = any(Transaction.class);
        transactionStub.save(matchedTransaction);
    }

    private Object recordWrite(InvocationOnMock invocation) {
        boolean active = TransactionSynchronizationManager.isActualTransactionActive();
        activeWrites.add(active);
        return invocation.getArgument(0);
    }

    private SaleCommand saleCommand() {
        return new SaleCommand(MOVEMENT_DATE, "Test sale", ITEMS_COUNT, SALE_AMOUNT);
    }

    private DebtForgivenessCommand forgivenessCommand() {
        return new DebtForgivenessCommand(MOVEMENT_DATE, "Test forgiveness");
    }

    private PersistedMovement captureWrites() {
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        ArgumentCaptor<Statistic> statisticCaptor = ArgumentCaptor.forClass(Statistic.class);
        InOrder order = inOrder(customerRepository, transactionRepository, statisticRepository);
        order.verify(customerRepository).save(customer);
        TransactionRepository transactionVerification = order.verify(transactionRepository);
        Transaction matchedTransaction = transactionCaptor.capture();
        transactionVerification.save(matchedTransaction);
        StatisticRepository statisticVerification = order.verify(statisticRepository);
        Statistic matchedStatistic = statisticCaptor.capture();
        statisticVerification.save(matchedStatistic);
        Transaction transaction = transactionCaptor.getValue();
        Statistic statistic = statisticCaptor.getValue();
        return new PersistedMovement(transaction, statistic);
    }

    private void assertMovement(PersistedMovement movement, TransactionType type, int amount, int balance) {
        Transaction transaction = movement.transaction();
        Statistic statistic = movement.statistic();
        TransactionType transactionType = transaction.getType();
        TransactionType statisticType = statistic.getType();
        Integer transactionAmount = transaction.getAmount();
        Integer statisticAmount = statistic.getAmount();
        Integer transactionBalance = transaction.getBalance();
        Integer debt = customer.getDebt();
        LocalDate transactionDate = transaction.getDate();
        LocalDate statisticDate = statistic.getDate();
        OffsetDateTime createdAt = transaction.getCreatedAt();
        assertThat(transactionType).isEqualTo(type);
        assertThat(statisticType).isEqualTo(type);
        assertThat(transactionAmount).isEqualTo(amount);
        assertThat(statisticAmount).isEqualTo(amount);
        assertThat(transactionBalance).isEqualTo(balance);
        assertThat(debt).isEqualTo(balance);
        assertThat(transactionDate).isEqualTo(MOVEMENT_DATE);
        assertThat(statisticDate).isEqualTo(MOVEMENT_DATE);
        assertThat(createdAt).isNotNull();
    }

    private void assertCommittedWriteTransaction() {
        int begins = transactionManager.getBeginCount();
        int commits = transactionManager.getCommitCount();
        int rollbacks = transactionManager.getRollbackCount();
        boolean readOnly = transactionManager.isReadOnly();
        assertThat(activeWrites).containsExactly(true, true, true);
        assertThat(begins).isEqualTo(1);
        assertThat(commits).isEqualTo(1);
        assertThat(rollbacks).isZero();
        assertThat(readOnly).isFalse();
    }

    private record PersistedMovement(Transaction transaction, Statistic statistic) {}
}
