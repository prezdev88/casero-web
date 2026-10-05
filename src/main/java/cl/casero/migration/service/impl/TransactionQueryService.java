package cl.casero.migration.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.repository.TransactionRepository;
import cl.casero.migration.repository.TransactionRepository.TopCustomerProjection;
import cl.casero.migration.service.TransactionQueries;
import cl.casero.migration.service.dto.TopCustomerSummary;
import cl.casero.migration.service.dto.TransactionMonthlySummary;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransactionQueryService implements TransactionQueries {

    private static final int DEFAULT_SUMMARY_MONTH_OFFSET = 5;
    private static final int FIRST_DAY_OF_MONTH = 1;

    private final TransactionRepository transactionRepository;
    private final Clock reportClock;

    @Override
    public Page<Transaction> listAll(TransactionType type, Pageable pageable) {
        if (type == null) {
            return transactionRepository.findAllVisible(pageable);
        }

        return transactionRepository.findVisibleByType(type, pageable);
    }

    @Override
    public Page<Transaction> listByCustomer(Long customerId, Pageable pageable) {
        return transactionRepository.findVisibleByCustomerId(customerId, pageable);
    }

    @Override
    public List<Transaction> listAllByCustomer(Long customerId) {
        return transactionRepository.findVisibleByCustomerIdOrderByDateDescIdDesc(customerId);
    }

    @Override
    public List<Transaction> listRecentByCustomer(Long customerId, int limit) {
        List<Transaction> transactions = transactionRepository.findVisibleByCustomerIdOrderByDateDescIdDesc(customerId);

        if (limit <= 0 || transactions.size() <= limit) {
            return transactions;
        }

        List<Transaction> firstTransactions = transactions.subList(0, limit);
        return new ArrayList<>(firstTransactions);
    }

    @Override
    public List<TransactionMonthlySummary> getMonthlySummary(LocalDate start, LocalDate end) {
        LocalDate endBase = (end != null) ? end : LocalDate.now(reportClock);
        LocalDate startBase = (start != null) ? start : endBase.minusMonths(DEFAULT_SUMMARY_MONTH_OFFSET);

        if (startBase.isAfter(endBase)) {
            LocalDate previousStart = startBase;
            startBase = endBase;
            endBase = previousStart;
        }

        LocalDate sanitizedStart = startBase.withDayOfMonth(FIRST_DAY_OF_MONTH);
        int lastDayOfMonth = endBase.lengthOfMonth();
        LocalDate sanitizedEnd = endBase.withDayOfMonth(lastDayOfMonth);
        List<Transaction> transactions = transactionRepository.findVisibleByDateBetween(sanitizedStart, sanitizedEnd);
        Map<YearMonth, List<Transaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(this::transactionMonth));
        YearMonth firstMonth = YearMonth.from(sanitizedStart);
        YearMonth lastMonth = YearMonth.from(sanitizedEnd);
        List<Transaction> emptyMonth = List.of();
        List<TransactionMonthlySummary> summaries = new ArrayList<>();

        for (YearMonth month = firstMonth; !month.isAfter(lastMonth); month = month.plusMonths(1)) {
            List<Transaction> monthTransactions = grouped.getOrDefault(month, emptyMonth);
            long sales = sumAmounts(monthTransactions, TransactionType.SALE);
            long payments = sumAmounts(monthTransactions, TransactionType.PAYMENT);
            LocalDate monthStart = month.atDay(FIRST_DAY_OF_MONTH);
            TransactionMonthlySummary summary = new TransactionMonthlySummary(monthStart, sales, payments);
            summaries.add(summary);
        }

        return summaries;
    }

    @Override
    public List<Transaction> getFinishedCardsThisMonth() {
        MonthRange range = currentMonth();
        LocalDate start = range.start();
        LocalDate end = range.end();
        return transactionRepository.findFinishedCards(start, end);
    }

    @Override
    public List<Transaction> getSalesThisMonth() {
        MonthRange range = currentMonth();
        LocalDate start = range.start();
        LocalDate end = range.end();
        return transactionRepository.findSalesThisMonth(start, end);
    }

    @Override
    public List<TopCustomerSummary> getTopCustomersThisMonth() {
        MonthRange range = currentMonth();
        LocalDate start = range.start();
        LocalDate end = range.end();
        List<TopCustomerProjection> topCustomers = transactionRepository.findTopCustomers(start, end);
        return topCustomers.stream()
                .map(this::toTopCustomerSummary)
                .toList();
    }

    @Override
    public long getSalesSum(LocalDate start, LocalDate end) {
        Integer sum = transactionRepository.sumByTypeAndDateRange(TransactionType.SALE, start, end);
        return (sum != null) ? sum : 0;
    }

    @Override
    public long getPaymentsSum(LocalDate start, LocalDate end) {
        Integer sum = transactionRepository.sumByTypeAndDateRange(TransactionType.PAYMENT, start, end);
        return (sum != null) ? sum : 0;
    }

    private YearMonth transactionMonth(Transaction transaction) {
        LocalDate date = transaction.getDate();
        return YearMonth.from(date);
    }

    private TopCustomerSummary toTopCustomerSummary(TopCustomerProjection projection) {
        String customerName = projection.getCustomerName();
        Integer totalPaid = projection.getTotalPaid();
        return new TopCustomerSummary(customerName, totalPaid);
    }

    private long sumAmounts(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    private MonthRange currentMonth() {
        LocalDate today = LocalDate.now(reportClock);
        LocalDate start = today.withDayOfMonth(FIRST_DAY_OF_MONTH);
        int lastDayOfMonth = today.lengthOfMonth();
        LocalDate end = today.withDayOfMonth(lastDayOfMonth);
        return new MonthRange(start, end);
    }

    private record MonthRange(LocalDate start, LocalDate end) {}
}
