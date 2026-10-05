package cl.casero.migration.web.presentation;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import cl.casero.migration.service.dto.TransactionMonthlySummary;

@Component
public class DashboardChartPresenter {

    private static final Locale SPANISH = Locale.forLanguageTag("es-ES");

    public DashboardChartData present(List<TransactionMonthlySummary> series) {
        List<String> labels = new ArrayList<>();
        List<Long> sales = new ArrayList<>();
        List<Long> payments = new ArrayList<>();
        for (TransactionMonthlySummary summary : series) {
            LocalDate date = summary.month();
            Month month = date.getMonth();
            String monthName = month.getDisplayName(TextStyle.SHORT, SPANISH);
            String firstLetter = monthName.substring(0, 1).toUpperCase();
            String suffix = monthName.substring(1);
            int year = date.getYear();
            labels.add(firstLetter + suffix + " " + year);
            long salesAmount = summary.salesAmount();
            long paymentsAmount = summary.paymentsAmount();
            sales.add(salesAmount);
            payments.add(paymentsAmount);
        }

        return new DashboardChartData(labels, sales, payments);
    }
}
