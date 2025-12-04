package cl.casero.migration.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record CustomerScoreSummary(double score, List<CycleScore> cycles) {

    public CustomerScoreSummary {
        List<CycleScore> safeCycles = cycles == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(cycles));
        cycles = safeCycles;
    }

    public boolean hasCycles() {
        return !cycles.isEmpty();
    }

    public record CycleScore(
        int cycleNumber,
        LocalDate cycleStartDate,
        LocalDate cycleEndDate,
        CustomerScoreCalculator.ScoreResult result
    ) {}
}
