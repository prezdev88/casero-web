package cl.casero.migration.util;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class CustomerScoreColor {

    public static String bucket(Double score) {
        if (score == null) {
            return "neutral";
        }
        if (score >= 6.5) {
            return "excellent";
        }
        if (score >= 5.5) {
            return "great";
        }
        if (score >= 4.5) {
            return "good";
        }
        if (score >= 3.5) {
            return "fair";
        }
        if (score >= 2.5) {
            return "warning";
        }
        return "risk";
    }
}
