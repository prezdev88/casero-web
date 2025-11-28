package cl.casero.migration.api.dto;

public record ScoreResultResponse(double score,
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
                                  Integer cycleMonths) {
}
