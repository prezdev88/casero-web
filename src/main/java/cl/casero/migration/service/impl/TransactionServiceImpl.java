package cl.casero.migration.service.impl;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Statistic;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.SaleType;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.repository.StatisticRepository;
import cl.casero.migration.repository.TransactionRepository;
import cl.casero.migration.service.TransactionService;
import cl.casero.migration.service.dto.DebtForgivenessForm;
import cl.casero.migration.service.dto.MoneyTransactionForm;
import cl.casero.migration.service.dto.PaymentForm;
import cl.casero.migration.service.dto.SaleForm;
import cl.casero.migration.service.dto.TransactionMonthlySummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Santiago");

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final StatisticRepository statisticRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  CustomerRepository customerRepository,
                                  StatisticRepository statisticRepository) {
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
        this.statisticRepository = statisticRepository;
    }

    @Override
    public Page<Transaction> listAll(TransactionType type, Pageable pageable) {
        if (type == null) {
            return transactionRepository.findAll(pageable);
        }
        return transactionRepository.findByType(type, pageable);
    }

    @Override
    public Page<Transaction> listByCustomer(Long customerId, Pageable pageable) {
        return transactionRepository.findByCustomerId(customerId, pageable);
    }

    @Override
    public List<Transaction> listAllByCustomer(Long customerId) {
        return transactionRepository.findByCustomerIdOrderByDateDescIdDesc(customerId);
    }

    @Override
    public void registerSale(Long customerId, SaleForm form) {
        Customer customer = getCustomer(customerId);
        int previousBalance = customer.getDebt();
        int newBalance = previousBalance + form.getAmount();
        SaleType saleType = previousBalance == 0 ? SaleType.NEW_SALE : SaleType.MAINTENANCE;

        Transaction transaction = buildTransaction(customer, form.getDate(), form.getDetail(),
                form.getAmount(), TransactionType.SALE, newBalance);
        transaction.setItemCount(form.getItemsCount());

        persistTransaction(transaction, form.getItemsCount(), saleType);
    }

    @Override
    public void registerPayment(Long customerId, PaymentForm form) {
        Customer customer = getCustomer(customerId);
        int newBalance = Math.max(0, customer.getDebt() - form.getAmount());

        Transaction transaction = buildTransaction(
                customer,
                form.getDate(),
                "[Abono]: $" + form.getAmount(),
                form.getAmount(),
                TransactionType.PAYMENT,
                newBalance
        );

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
        int newBalance = 0;

        Transaction transaction = buildTransaction(customer, form.getDate(), form.getDetail(),
                amount, TransactionType.DEBT_FORGIVENESS, newBalance);

        persistTransaction(transaction, null, null);
    }

    @Override
    public void delete(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();
        Customer customer = transaction.getCustomer();

        statisticRepository.deleteByTypeAndAmountAndDate(transaction.getType(),
                transaction.getAmount(), transaction.getDate());

        transactionRepository.delete(transaction);

        Transaction lastTransaction = transactionRepository
                .findTopByCustomerIdOrderByCreatedAtDescIdDesc(customer.getId());
        int recalculatedDebt = lastTransaction != null ? lastTransaction.getBalance() : 0;
        customer.setDebt(recalculatedDebt);
        customerRepository.save(customer);
    }

    private void registerMoneyFlow(Long customerId, MoneyTransactionForm form, TransactionType type) {
        Customer customer = getCustomer(customerId);
        int newBalance = Math.max(0, customer.getDebt() - form.getAmount());

        Transaction transaction = buildTransaction(customer, form.getDate(), form.getDetail(),
                form.getAmount(), type, newBalance);

        persistTransaction(transaction, null, null);
    }

    private Customer getCustomer(Long customerId) {
        return customerRepository.findById(customerId).orElseThrow();
    }

    private Transaction buildTransaction(Customer customer,
                                          LocalDate date,
                                          String detail,
                                          int amount,
                                          TransactionType type,
                                          int newBalance) {
        Transaction transaction = new Transaction();
        transaction.setCustomer(customer);
        transaction.setDate(date);
        transaction.setDetail(detail);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setBalance(newBalance);
        transaction.setCreatedAt(OffsetDateTime.now(DEFAULT_ZONE));
        return transaction;
    }

    private void persistTransaction(Transaction transaction, Integer itemsCount, SaleType saleType) {
        Customer customer = transaction.getCustomer();
        customer.setDebt(transaction.getBalance());
        customerRepository.save(customer);

        transactionRepository.save(transaction);

        Statistic statistic = new Statistic();
        statistic.setType(transaction.getType());
        statistic.setAmount(transaction.getAmount());
        statistic.setDate(transaction.getDate());
        statistic.setItemsCount(itemsCount);
        statistic.setSaleType(saleType);
        statisticRepository.save(statistic);
    }

    @Override
    public List<TransactionMonthlySummary> getMonthlySummary(LocalDate start, LocalDate end) {
        LocalDate endBase = end != null ? end : LocalDate.now();
        LocalDate startBase = start != null ? start : endBase.minusMonths(5);
        if (startBase.isAfter(endBase)) {
            LocalDate tmp = startBase;
            startBase = endBase;
            endBase = tmp;
        }

        LocalDate sanitizedStart = startBase.withDayOfMonth(1);
        LocalDate sanitizedEnd = endBase.withDayOfMonth(endBase.lengthOfMonth());

        List<Transaction> transactions = transactionRepository.findByDateBetween(sanitizedStart, sanitizedEnd);
        Map<YearMonth, List<Transaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(tx -> YearMonth.from(tx.getDate())));

        List<TransactionMonthlySummary> result = new ArrayList<>();
        for (YearMonth ym = YearMonth.from(sanitizedStart); !ym.isAfter(YearMonth.from(sanitizedEnd)); ym = ym.plusMonths(1)) {
            List<Transaction> monthTransactions = grouped.getOrDefault(ym, List.of());
            long sales = monthTransactions.stream()
                    .filter(tx -> tx.getType() == TransactionType.SALE)
                    .mapToLong(Transaction::getAmount)
                    .sum();
            long payments = monthTransactions.stream()
                    .filter(tx -> tx.getType() == TransactionType.PAYMENT)
                    .mapToLong(Transaction::getAmount)
                    .sum();
            result.add(new TransactionMonthlySummary(ym.atDay(1), sales, payments));
        }
        return result;
    }
}
