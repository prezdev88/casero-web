package cl.casero.migration.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class CustomerScoreCalculator {

    private static final double MAX_SCORE = 7.0;
    private static final double MIN_SCORE = 1.0;
    private static final int PERFECT_PAYMENT_WINDOW_DAYS = 45;

    public static double calculateScore(ScoreInputs inputs) {
        return evaluate(inputs).score();
    }

    public static ScoreResult evaluate(ScoreInputs inputs) {
        if (inputs == null) {
            return new ScoreResult(MIN_SCORE, null, null, null, 0, 0, false, MIN_SCORE, MIN_SCORE, MIN_SCORE, 1.0, false, null, null);
        }
        boolean hasPayments = inputs.totalPayments() > 0;
        boolean hasOutstandingDebt = inputs.hasOutstandingDebt();
        Integer daysSinceLast = daysSince(inputs.lastPaymentDate());
        Integer effectiveCurrentDelay = hasOutstandingDebt ? daysSinceLast : null;
        Integer historicalMaxInterval = inputs.maxIntervalBetweenPayments();
        Integer averageInterval = averageInterval(inputs.totalIntervalDays(), inputs.intervalCount());
        double latestScore = scoreFromDelay(effectiveCurrentDelay, hasPayments);
        double averageScore = scoreFromDelay(averageInterval, hasPayments);
        double coverageScore = coverageScore(inputs.paymentMonthCount(), inputs.cycleMonthCount(), hasPayments);
        double baseScore = hasOutstandingDebt
                ? Math.min(Math.min(latestScore, averageScore), coverageScore)
                : coverageScore;
        double historyFactor = computeHistoryFactor(
                inputs.lateIntervalCount(),
                inputs.intervalCount(),
                historicalMaxInterval);
        double finalScore = roundTwoDecimals(clamp(baseScore * (hasOutstandingDebt ? historyFactor : 1.0), MIN_SCORE, MAX_SCORE));
        return new ScoreResult(finalScore,
                daysSinceLast,
                historicalMaxInterval,
                averageInterval,
                inputs.lateIntervalCount(),
                inputs.intervalCount(),
                hasPayments,
                latestScore,
                averageScore,
                coverageScore,
                historyFactor,
                hasOutstandingDebt,
                inputs.paymentMonthCount() != null ? inputs.paymentMonthCount().intValue() : null,
                inputs.cycleMonthCount());
    }

    public static double minScore() {
        return MIN_SCORE;
    }

    private static double scoreFromDelay(Integer delayInDays, boolean hasPayments) {
        if (!hasPayments) {
            return MIN_SCORE;
        }
        if (delayInDays == null) {
            return MAX_SCORE;
        }
        if (delayInDays <= PERFECT_PAYMENT_WINDOW_DAYS) {
            return MAX_SCORE;
        }
        // Keep a perfect score for payments within the window and decay proportionally afterwards.
        double ratio = (double) PERFECT_PAYMENT_WINDOW_DAYS / (double) delayInDays;
        double score = MAX_SCORE * ratio;
        return roundTwoDecimals(clamp(score, MIN_SCORE, MAX_SCORE));
    }
    private static double computeHistoryFactor(
        Integer lateIntervalCount,
        Integer intervalCount,
        Integer maxInterval
    ) {
        int totalIntervals = intervalCount == null ? 0 : intervalCount;
        int lateIntervals = lateIntervalCount == null ? 0 : lateIntervalCount;
        if (totalIntervals <= 0) {
            return 1.0;
        }
        double lateFrequency = (double) lateIntervals / (double) totalIntervals;
        double severityModifier = 1.0;
        if (maxInterval != null && maxInterval > PERFECT_PAYMENT_WINDOW_DAYS) {
            severityModifier = clampRatio(
                    (double) PERFECT_PAYMENT_WINDOW_DAYS / (double) maxInterval,
                    0.1,
                    1.0);
        }
        double penalty = lateFrequency * (1.0 - severityModifier);
        double factor = 1.0 - penalty;
        return clampRatio(factor, 0.3, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampRatio(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static Integer daysSince(LocalDate date) {
        if (date == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(date, LocalDate.now());
        return (int) Math.max(days, 0);
    }

    private static Integer averageInterval(Long totalIntervalDays, Integer intervalCount) {
        if (totalIntervalDays == null || intervalCount == null || intervalCount <= 0) {
            return null;
        }
        return (int) Math.max(totalIntervalDays / intervalCount, 0);
    }

    private static double coverageScore(Long paymentMonths, Integer cycleMonths, boolean hasPayments) {
        if (!hasPayments) {
            return MIN_SCORE;
        }
        if (paymentMonths == null || cycleMonths == null || cycleMonths <= 0) {
            return MAX_SCORE;
        }
        int allowedSkips = Math.min(2, Math.max(cycleMonths / 4, 1));
        int effectiveMonths = Math.max(1, cycleMonths - allowedSkips);
        double adjustedRatio = (double) Math.min(paymentMonths, cycleMonths) / (double) effectiveMonths;
        adjustedRatio = Math.max(0.0, Math.min(1.0, adjustedRatio));
        return roundTwoDecimals(MIN_SCORE + (adjustedRatio * (MAX_SCORE - MIN_SCORE)));
    }

    public record ScoreInputs(
        int totalPayments,
        LocalDate lastPaymentDate,
        Integer maxIntervalBetweenPayments,
        Long totalIntervalDays,
        Integer intervalCount,
        Integer lateIntervalCount,
        Long paymentMonthCount,
        Integer cycleMonthCount,
        boolean hasOutstandingDebt
    ) {}

    public record ScoreResult(
        double score,
        Integer daysSinceLastPayment,
        Integer maxIntervalBetweenPayments,
        Integer averageIntervalBetweenPayments,
        Integer lateIntervals,
        Integer totalIntervals,
        boolean hasPayments,
        double latestScoreComponent,
        double averageScoreComponent,
        double coverageScoreComponent,
        double historyFactor,
        boolean hasOutstandingDebt,
        Integer paymentMonths,
        Integer cycleMonths
    ) {}
}
