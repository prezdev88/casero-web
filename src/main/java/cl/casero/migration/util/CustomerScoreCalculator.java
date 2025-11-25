package cl.casero.migration.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class CustomerScoreCalculator {

    private static final double MAX_SCORE = 7.0;
    private static final double MIN_SCORE = 1.0;
    private static final double SCORE_RANGE = MAX_SCORE - MIN_SCORE;

    private static final int DEBT_MAX_THRESHOLD = 120_000;
    private static final int PAYMENT_RECENCY_WINDOW_DAYS = 150;
    private static final int ACTIVITY_RECENCY_WINDOW_DAYS = 120;

    private static final double COVERAGE_WEIGHT = 0.35;
    private static final double DEBT_WEIGHT = 0.30;
    private static final double PAYMENT_RECENCY_WEIGHT = 0.25;
    private static final double ACTIVITY_RECENCY_WEIGHT = 0.10;

    private CustomerScoreCalculator() {
    }

    public static double calculateScore(ScoreInputs inputs) {
        if (inputs == null) {
            return MIN_SCORE;
        }
        double coverageComponent = coverageScore(inputs.totalCharges(), inputs.totalPayments());
        double debtComponent = inverseLinear(inputs.debt(), DEBT_MAX_THRESHOLD);
        double paymentRecencyComponent = recencyScore(inputs.lastPaymentDate(), PAYMENT_RECENCY_WINDOW_DAYS);
        double activityComponent = recencyScore(inputs.lastActivityDate(), ACTIVITY_RECENCY_WINDOW_DAYS);

        double weighted = coverageComponent * COVERAGE_WEIGHT
                + debtComponent * DEBT_WEIGHT
                + paymentRecencyComponent * PAYMENT_RECENCY_WEIGHT
                + activityComponent * ACTIVITY_RECENCY_WEIGHT;

        double score = MIN_SCORE + (weighted * SCORE_RANGE);
        return roundTwoDecimals(clamp(score, MIN_SCORE, MAX_SCORE));
    }

    public static double minScore() {
        return MIN_SCORE;
    }

    private static double coverageScore(int totalCharges, int totalPayments) {
        if (totalCharges <= 0) {
            return 1.0;
        }
        double ratio = (double) totalPayments / (double) totalCharges;
        return clamp(ratio, 0.0, 1.0);
    }

    private static double recencyScore(LocalDate date, int windowDays) {
        if (date == null) {
            return 0.0;
        }
        long days = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (days <= 0) {
            return 1.0;
        }
        if (days >= windowDays) {
            return 0.0;
        }
        return 1.0 - ((double) days / (double) windowDays);
    }

    private static double inverseLinear(int value, int max) {
        if (value <= 0) {
            return 1.0;
        }
        if (value >= max) {
            return 0.0;
        }
        return 1.0 - ((double) value / (double) max);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record ScoreInputs(int debt,
                              int totalCharges,
                              int totalPayments,
                              LocalDate lastPaymentDate,
                              LocalDate lastActivityDate) {
    }
}
