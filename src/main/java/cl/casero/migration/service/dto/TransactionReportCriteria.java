package cl.casero.migration.service.dto;

import java.util.Objects;

import cl.casero.migration.domain.enums.TransactionType;

/** For MONTHS, a missing count uses the default; a missing type includes all categories. */
public record TransactionReportCriteria(ReportRange range, Integer months, TransactionType filterType) {

    public TransactionReportCriteria {
        Objects.requireNonNull(range, "Report range is required");
    }

    public enum ReportRange {
        ALL,
        MONTHS
    }
}
