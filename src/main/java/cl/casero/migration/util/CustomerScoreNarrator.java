package cl.casero.migration.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class CustomerScoreNarrator {

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMM yyyy").withLocale(new Locale("es", "ES"));

    private CustomerScoreNarrator() {}

    public static String buildExplanation(CustomerScoreSummary summary) {
        if (summary == null) {
            return "";
        }

        if (!summary.hasCycles()) {
            return "No registra ciclos de compra evaluables, aún no hay historial para analizar.";
        }

        StringBuilder explanation = new StringBuilder();
        int cycleCount = summary.cycles().size();

        explanation.append("Se analizaron ")
                .append(cycleCount)
                .append(cycleCount == 1 ? " ciclo." : " ciclos.");

        for (int i = cycleCount - 1; i >= 0; i--) {
            CustomerScoreSummary.CycleScore cycle = summary.cycles().get(i);
            explanation.append(' ').append(describeCycle(cycle));
        }

        return explanation.toString();
    }

    private static String describeCycle(CustomerScoreSummary.CycleScore cycle) {
        CustomerScoreCalculator.ScoreResult result = cycle.result();
        StringBuilder cycleDescription = new StringBuilder("Ciclo ");
        cycleDescription.append(cycle.cycleNumber());
        String rangeText = formatRange(cycle.cycleStartDate(), cycle.cycleEndDate());

        if (!rangeText.isEmpty()) {
            cycleDescription.append(' ').append(rangeText);
        }

        cycleDescription.append(": nota ")
                .append(String.format(Locale.US, "%.2f", result.score()))
                .append(", ");

        if (!result.hasPayments()) {
            cycleDescription.append("sin pagos registrados");
        } else if (result.paymentMonths() != null && result.cycleMonths() != null && result.cycleMonths() > 0) {
            cycleDescription.append("pagó en ")
                    .append(result.paymentMonths())
                    .append(" de ")
                    .append(result.cycleMonths())
                    .append(" meses");
        } else {
            cycleDescription.append("historial parcial de pagos");
        }

        if (result.hasOutstandingDebt()) {
            cycleDescription.append(" y mantiene saldo pendiente");
        } else {
            cycleDescription.append(" y dejó el ciclo al día");
        }

        Integer lastPaymentDays = result.daysSinceLastPayment();

        if (lastPaymentDays != null) {
            cycleDescription.append(" (último pago ").append(elapsedText(lastPaymentDays)).append(')');
        }

        cycleDescription.append('.');

        return cycleDescription.toString();
    }

    private static String formatRange(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return "";
        }

        if (start != null && end != null) {
            return "(" + formatMonth(start) + " - " + formatMonth(end) + ")";
        }

        if (start != null) {
            return "(desde " + formatMonth(start) + ")";
        }

        return "(hasta " + formatMonth(end) + ")";
    }

    private static String formatMonth(LocalDate date) {
        return date == null ? "" : MONTH_FORMATTER.format(date);
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
