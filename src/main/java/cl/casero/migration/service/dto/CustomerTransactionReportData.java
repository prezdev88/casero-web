package cl.casero.migration.service.dto;

import java.util.List;

import cl.casero.migration.domain.enums.TransactionType;

public record CustomerTransactionReportData(
    ReportCustomerData customer,
    List<ReportTransactionData> transactions,
    String rangeLabel,
    TransactionType filterType
) {

    public CustomerTransactionReportData {
        transactions = List.copyOf(transactions);
    }
}
