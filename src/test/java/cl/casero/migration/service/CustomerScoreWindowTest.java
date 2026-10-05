package cl.casero.migration.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cl.casero.migration.util.CustomerScoreCalculator;
import cl.casero.migration.util.CustomerScoreCalculator.ScoreInputs;
import cl.casero.migration.util.CustomerScoreCalculator.ScoreResult;

class CustomerScoreWindowTest {

    private static final int PERFECT_WINDOW = 45;

    @ParameterizedTest
    @CsvSource({"0, 7.0", "45, 7.0", "46, 6.85", "90, 3.5"})
    void retainsThePerfectWindowBoundaryAndProportionalDecay(long intervalDays, double expectedScore) {
        ScoreInputs inputs = new ScoreInputs(1, null, null, intervalDays, 1, 0, null, null, true);

        ScoreResult result = CustomerScoreCalculator.evaluate(inputs);

        double score = result.score();
        int window = CustomerScoreCalculator.perfectPaymentWindowDays();
        assertThat(score).isEqualTo(expectedScore);
        assertThat(window).isEqualTo(PERFECT_WINDOW);
    }
}
