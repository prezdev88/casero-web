package cl.casero.migration.service.dto;

import java.util.List;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;

public record CustomerTransactionReportData(
    Customer customer,
    List<Transaction> transactions,
    String rangeLabel,
    TransactionType filterType
) {

    public CustomerTransactionReportData {
        transactions = List.copyOf(transactions);
    }
}
