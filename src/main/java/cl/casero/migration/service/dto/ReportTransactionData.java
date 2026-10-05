package cl.casero.migration.service.dto;

import java.time.LocalDate;

import cl.casero.migration.domain.enums.TransactionType;

public record ReportTransactionData(
    LocalDate date,
    TransactionType type,
    String detail,
    Integer amount,
    Integer balance
) {}
