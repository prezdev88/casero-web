package cl.casero.migration.service.impl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Statistic;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.SaleType;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.repository.StatisticRepository;
import cl.casero.migration.repository.TransactionRepository;
import cl.casero.migration.service.CustomerNotFoundException;
import cl.casero.migration.service.TransactionCommands;
import cl.casero.migration.service.dto.DebtForgivenessForm;
import cl.casero.migration.service.dto.MoneyTransactionForm;
import cl.casero.migration.service.dto.PaymentForm;
import cl.casero.migration.service.dto.SaleForm;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionCommandService implements TransactionCommands {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Santiago");

    private final CustomerRepository customerRepository;
    private final StatisticRepository statisticRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public void registerSale(Long customerId, SaleForm form) {
        Customer customer = getCustomer(customerId);
        int previousBalance = customer.getDebt();
        int amount = form.getAmount();
        int newBalance = previousBalance + amount;
        SaleType saleType = (previousBalance == 0) ? SaleType.NEW_SALE : SaleType.MAINTENANCE;
        LocalDate date = form.getDate();
        String detail = form.getDetail();
        Integer itemsCount = form.getItemsCount();
        Transaction transaction = buildTransaction(customer, date, detail, amount, TransactionType.SALE, newBalance);
        transaction.setItemCount(itemsCount);

        persistTransaction(transaction, itemsCount, saleType);
    }

    @Override
    public void registerPayment(Long customerId, PaymentForm form) {
        Customer customer = getCustomer(customerId);
        int debt = customer.getDebt();
        int amount = form.getAmount();
        int newBalance = Math.max(0, debt - amount);
        LocalDate date = form.getDate();
        String detail = "[Abono]: $" + amount;
        Transaction transaction = buildTransaction(customer, date, detail, amount, TransactionType.PAYMENT, newBalance);

        persistTransaction(transaction, null, null);
    }

    @Override
    public void registerRefund(Long customerId, MoneyTransactionForm form) {
        registerMoneyFlow(customerId, form, TransactionType.REFUND);
    }

    @Override
    public void registerFaultDiscount(Long customerId, MoneyTransactionForm form) {
        registerMoneyFlow(customerId, form, TransactionType.FAULT_DISCOUNT);
    }

    @Override
    public void forgiveDebt(Long customerId, DebtForgivenessForm form) {
        Customer customer = getCustomer(customerId);
        int amount = customer.getDebt();

        if (amount <= 0) {
            return;
        }

        LocalDate date = form.getDate();
        String detail = form.getDetail();
        Transaction transaction = buildTransaction(customer, date, detail, amount, TransactionType.DEBT_FORGIVENESS, 0);

        persistTransaction(transaction, null, null);
    }

    @Override
    public void delete(Long transactionId) {
        Optional<Transaction> result = transactionRepository.findById(transactionId);
        Transaction transaction = result.orElseThrow();
        Customer customer = transaction.getCustomer();
        TransactionType type = transaction.getType();
        Integer amount = transaction.getAmount();
        LocalDate date = transaction.getDate();
        statisticRepository.deleteByTypeAndAmountAndDate(type, amount, date);
        transactionRepository.delete(transaction);

        Long customerId = customer.getId();
        Pageable latestPage = Pageable.ofSize(1);
        Page<Transaction> latestTransactions = transactionRepository.findLatestVisibleByCustomerId(customerId, latestPage);
        Transaction lastTransaction = latestTransactions.stream().findFirst().orElse(null);
        int recalculatedDebt = (lastTransaction != null) ? lastTransaction.getBalance() : 0;
        customer.setDebt(recalculatedDebt);
        customerRepository.save(customer);
    }

    private void registerMoneyFlow(Long customerId, MoneyTransactionForm form, TransactionType type) {
        Customer customer = getCustomer(customerId);
        int debt = customer.getDebt();
        int amount = form.getAmount();
        int newBalance = Math.max(0, debt - amount);
        LocalDate date = form.getDate();
        String detail = form.getDetail();
        Transaction transaction = buildTransaction(customer, date, detail, amount, type, newBalance);

        persistTransaction(transaction, null, null);
    }

    private Customer getCustomer(Long customerId) {
        return customerRepository.findByIdAndEnabledTrue(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    private Transaction buildTransaction(
        Customer customer,
        LocalDate date,
        String detail,
        int amount,
        TransactionType type,
        int newBalance
    ) {
        Transaction transaction = new Transaction();
        transaction.setCustomer(customer);
        transaction.setDate(date);
        transaction.setDetail(detail);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setBalance(newBalance);
        OffsetDateTime createdAt = OffsetDateTime.now(DEFAULT_ZONE);
        transaction.setCreatedAt(createdAt);

        return transaction;
    }

    private void persistTransaction(Transaction transaction, Integer itemsCount, SaleType saleType) {
        Customer customer = transaction.getCustomer();
        Integer balance = transaction.getBalance();
        customer.setDebt(balance);
        customerRepository.save(customer);
        transactionRepository.save(transaction);

        TransactionType type = transaction.getType();
        Integer amount = transaction.getAmount();
        LocalDate date = transaction.getDate();
        Statistic statistic = new Statistic();
        statistic.setType(type);
        statistic.setAmount(amount);
        statistic.setDate(date);
        statistic.setItemsCount(itemsCount);
        statistic.setSaleType(saleType);
        statisticRepository.save(statistic);
    }
}
