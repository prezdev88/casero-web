package cl.casero.migration.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.dto.CustomerTransactionReportData;
import cl.casero.migration.service.dto.TransactionReportCriteria;
import cl.casero.migration.service.dto.TransactionReportCriteria.ReportRange;

@Service
@RequiredArgsConstructor
public class CustomerTransactionReportService {

    private static final int DEFAULT_REPORT_MONTHS = 12;
    private static final int MAX_REPORT_MONTHS = 60;

    private final CustomerService customerService;
    private final TransactionService transactionService;
    private final Clock reportClock;

    public CustomerTransactionReportData prepare(Long customerId, TransactionReportCriteria criteria) {
        Customer customer = customerService.get(customerId);
        List<Transaction> transactions = transactionService.listAllByCustomer(customerId);
        ReportRange range = criteria.range();
        String rangeLabel;

        if (range == ReportRange.MONTHS) {
            Integer requestedMonths = criteria.months();
            int months = sanitizeMonths(requestedMonths);
            transactions = filterByMonths(transactions, months);
            rangeLabel = "Últimos " + months + ((months == 1) ? " mes" : " meses");
        } else {
            rangeLabel = "Todas las transacciones";
        }

        TransactionType filterType = criteria.filterType();
        List<Transaction> selectedTransactions = filterByType(transactions, filterType);

        return new CustomerTransactionReportData(customer, selectedTransactions, rangeLabel, filterType);
    }

    private int sanitizeMonths(Integer requestedMonths) {
        if (requestedMonths == null || requestedMonths < 1) {
            return DEFAULT_REPORT_MONTHS;
        }

        return Math.min(requestedMonths, MAX_REPORT_MONTHS);
    }

    private List<Transaction> filterByMonths(List<Transaction> transactions, int months) {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now(reportClock);
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate cutoff = firstDayOfMonth.minusMonths(months - 1);

        return transactions.stream()
                .filter(transaction -> isOnOrAfter(transaction, cutoff))
                .toList();
    }

    private List<Transaction> filterByType(List<Transaction> transactions, TransactionType filterType) {
        if (filterType == null) {
            return transactions;
        }

        return transactions.stream()
                .filter(transaction -> transaction.getType() == filterType)
                .toList();
    }

    private boolean isOnOrAfter(Transaction transaction, LocalDate cutoff) {
        LocalDate date = transaction.getDate();
        return date != null && !date.isBefore(cutoff);
    }
}
