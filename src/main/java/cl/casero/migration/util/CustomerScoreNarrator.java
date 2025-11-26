package cl.casero.migration.util;

public final class CustomerScoreNarrator {

    private static final int IDEAL_PAYMENT_WINDOW_DAYS = 45;

    private CustomerScoreNarrator() {
    }

    public static String buildExplanation(CustomerScoreCalculator.ScoreResult summary) {
        if (summary == null) {
            return "";
        }
        if (!summary.hasPayments()) {
            return "No registra abonos, por lo que no podemos confiar en su comportamiento de pago.";
        }
        if (!summary.hasOutstandingDebt()) {
            StringBuilder noDebtMessage = new StringBuilder("No tiene saldo pendiente.");
            Integer lastPaymentDays = summary.daysSinceLastPayment();
            if (lastPaymentDays != null) {
                noDebtMessage.append(" Último pago registrado: ").append(elapsedText(lastPaymentDays)).append('.');
            }
            Integer averageInterval = summary.averageIntervalBetweenPayments();
            Integer worstInterval = summary.maxIntervalBetweenPayments();
            boolean hasHistoricalPenalty = summary.historyFactor() < 0.99;
            if (hasHistoricalPenalty && averageInterval != null && worstInterval != null) {
                noDebtMessage.append(" Sin embargo, sus pagos se tienden a espaciar alrededor de ")
                        .append(durationText(averageInterval))
                        .append(" y en su peor caso llegaron a ")
                        .append(durationText(worstInterval))
                        .append(". Eso explica una nota menor.");
            }
            return noDebtMessage.toString();
        }
        Integer days = summary.daysSinceLastPayment();
        if (days == null) {
            return "No hay registro reciente de pagos, se considera una nota de riesgo.";
        }
        StringBuilder explanation = new StringBuilder();
        Integer worstInterval = summary.maxIntervalBetweenPayments();
        boolean hasLongHistory = worstInterval != null && worstInterval > IDEAL_PAYMENT_WINDOW_DAYS;
        boolean hasHistoricalPenalty = summary.historyFactor() < 0.99 && hasLongHistory;
        if (hasHistoricalPenalty && days <= IDEAL_PAYMENT_WINDOW_DAYS) {
            explanation.append("El último pago llegó dentro de los 45 días, pero el cliente no mantiene ese ritmo.");
        } else {
            explanation.append(describeFrequency(days));
        }
        explanation.append(" Último pago: ").append(elapsedText(days)).append('.');
        if (hasHistoricalPenalty) {
            explanation.append(" Históricamente llegó a demorar ")
                    .append(durationText(worstInterval))
                    .append(" entre abonos, lo que disminuye su nota.");
        }
        return explanation.toString();
    }

    private static String describeFrequency(int days) {
        if (days <= IDEAL_PAYMENT_WINDOW_DAYS) {
            return "Paga dentro de los 45 días posteriores a la compra, comportamiento ideal.";
        }
        if (days <= 90) {
            return "Se demora cerca de dos meses en liquidar sus compras.";
        }
        if (days <= 180) {
            return "Sus pagos se están retrasando por varios meses.";
        }
        return "Lleva demasiados meses sin pagar, alto riesgo.";
    }

    private static String durationText(int days) {
        if (days <= 1) {
            return "1 día";
        }
        if (days < 30) {
            return days + " días";
        }
        int months = days / 30;
        if (months == 1) {
            return "1 mes";
        }
        if (months < 12) {
            return months + " meses";
        }
        int years = months / 12;
        if (years == 1) {
            return "1 año";
        }
        return years + " años";
    }

    private static String elapsedText(int days) {
        if (days <= 0) {
            return "hoy";
        }
        if (days == 1) {
            return "hace 1 día";
        }
        if (days < 30) {
            return "hace " + days + " días";
        }
        int months = days / 30;
        if (months == 1) {
            return "hace 1 mes";
        }
        if (months < 12) {
            return "hace " + months + " meses";
        }
        int years = months / 12;
        if (years == 1) {
            return "hace 1 año";
        }
        return "hace " + years + " años";
    }
}
