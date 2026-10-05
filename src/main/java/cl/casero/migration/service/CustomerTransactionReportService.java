package cl.casero.migration.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.dto.CustomerDetails;
import cl.casero.migration.service.dto.CustomerTransactionReportData;
import cl.casero.migration.service.dto.ReportCustomerData;
import cl.casero.migration.service.dto.ReportTransactionData;
import cl.casero.migration.service.dto.SectorSummary;
import cl.casero.migration.service.dto.TransactionDetails;
import cl.casero.migration.service.dto.TransactionReportCriteria;
import cl.casero.migration.service.dto.TransactionReportCriteria.ReportRange;

@Service
@RequiredArgsConstructor
public class CustomerTransactionReportService {

    private static final int DEFAULT_REPORT_MONTHS = 12;
    private static final int MAX_REPORT_MONTHS = 60;

    private final CustomerQueries customerQueries;
    private final TransactionQueries transactionQueries;
    private final Clock reportClock;

    public CustomerTransactionReportData prepare(Long customerId, TransactionReportCriteria criteria) {
        CustomerDetails customer = customerQueries.get(customerId);
        List<TransactionDetails> transactions = transactionQueries.listAllByCustomer(customerId);
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
        List<TransactionDetails> selectedTransactions = filterByType(transactions, filterType);

        ReportCustomerData reportCustomer = toReportCustomer(customer);
        List<ReportTransactionData> rows = selectedTransactions.stream()
                .map(this::toReportTransaction)
                .toList();
        return new CustomerTransactionReportData(reportCustomer, rows, rangeLabel, filterType);
    }

    private ReportCustomerData toReportCustomer(CustomerDetails customer) {
        String name = customer.getName();
        String address = customer.getAddress();
        SectorSummary sector = customer.getSector();
        String sectorName = (sector != null) ? sector.getName() : "No asignado";
        Integer debt = customer.getDebt();
        return new ReportCustomerData(name, address, sectorName, debt);
    }

    private ReportTransactionData toReportTransaction(TransactionDetails transaction) {
        LocalDate date = transaction.getDate();
        TransactionType type = transaction.getType();
        String detail = transaction.getDetail();
        Integer amount = transaction.getAmount();
        Integer balance = transaction.getBalance();
        return new ReportTransactionData(date, type, detail, amount, balance);
    }

    private int sanitizeMonths(Integer requestedMonths) {
        if (requestedMonths == null || requestedMonths < 1) {
            return DEFAULT_REPORT_MONTHS;
        }

        return Math.min(requestedMonths, MAX_REPORT_MONTHS);
    }

    private List<TransactionDetails> filterByMonths(List<TransactionDetails> transactions, int months) {
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

    private List<TransactionDetails> filterByType(List<TransactionDetails> transactions, TransactionType filterType) {
        if (filterType == null) {
            return transactions;
        }

        return transactions.stream()
                .filter(transaction -> transaction.getType() == filterType)
                .toList();
    }

    private boolean isOnOrAfter(TransactionDetails transaction, LocalDate cutoff) {
        LocalDate date = transaction.getDate();
        return date != null && !date.isBefore(cutoff);
    }
}
