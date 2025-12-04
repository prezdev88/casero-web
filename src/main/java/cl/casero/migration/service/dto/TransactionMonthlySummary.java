package cl.casero.migration.service.dto;

import java.time.LocalDate;

public record TransactionMonthlySummary(
    LocalDate month,
    long salesAmount,
    long paymentsAmount
) {}
