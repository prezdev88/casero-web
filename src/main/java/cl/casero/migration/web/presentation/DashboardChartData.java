package cl.casero.migration.web.presentation;

import java.util.List;

public record DashboardChartData(List<String> labels, List<Long> sales, List<Long> payments) {
    public DashboardChartData {
        labels = List.copyOf(labels);
        sales = List.copyOf(sales);
        payments = List.copyOf(payments);
    }
}
