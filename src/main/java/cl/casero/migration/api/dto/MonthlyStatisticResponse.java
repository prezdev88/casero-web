package cl.casero.migration.api.dto;

import cl.casero.migration.domain.MonthlyStatistic;
import java.time.LocalDate;

public record MonthlyStatisticResponse(MonthlyStatistic stats,
                                       LocalDate start,
                                       LocalDate end) {
}
