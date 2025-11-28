package cl.casero.migration.api.dto;

import java.time.LocalDate;

public record CustomerScoreCycleResponse(int cycleNumber,
                                         LocalDate startDate,
                                         LocalDate endDate,
                                         ScoreResultResponse result) {
}
