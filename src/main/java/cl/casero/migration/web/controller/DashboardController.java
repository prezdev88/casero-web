package cl.casero.migration.web.controller;

import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.StatisticsService;
import cl.casero.migration.service.TransactionService;
import cl.casero.migration.service.dto.TransactionMonthlySummary;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.time.format.TextStyle;
import java.util.Locale;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final CustomerService customerService;
    private final StatisticsService statisticsService;
    private final TransactionService transactionService;

    @GetMapping
    public String index(Model model) {
        LocalDate today = LocalDate.now();
        
        // 1. Upcoming Birthdays
        long upcomingBirthdaysCount = customerService.getUpcomingBirthdaysThisMonthCount(today.getMonthValue(), today.getDayOfMonth());
        
        // 2. Total Debt
        int totalDebt = statisticsService.getTotalDebt();
        
        // 3. Active Customers
        long activeCustomersCount = customerService.count();
        
        // 4. Overdue Customers (morosos) - defined as 1+ months overdue in the service layer
        long overdueCustomersCount = customerService.getOverdueCustomers(PageRequest.of(0, 1), 1).getTotalElements();
        
        // 5. Monthly Sales/Payments (Optimized)
        cl.casero.migration.domain.MonthlyStatistic stats = statisticsService.getMonthlyStatistic(today.getMonthValue(), today.getYear());
        long salesAmount = stats.getSalesCount(); // refers to sales sum
        long paymentsAmount = stats.getPaymentsCount(); // refers to payments sum

        model.addAttribute("upcomingBirthdaysCount", upcomingBirthdaysCount);
        model.addAttribute("totalDebt", totalDebt);
        model.addAttribute("activeCustomersCount", activeCustomersCount);
        model.addAttribute("overdueCustomersCount", overdueCustomersCount);
        model.addAttribute("salesAmount", salesAmount);
        model.addAttribute("paymentsAmount", paymentsAmount);
        
        // Extra stats
        int averageDebt = statisticsService.getAverageDebt();
        int finishedCardsCount = stats.getFinishedCardsCount();
        int totalItemsCount = stats.getTotalItemsCount();
        
        model.addAttribute("averageDebt", averageDebt);
        model.addAttribute("finishedCardsCount", finishedCardsCount);
        model.addAttribute("totalItemsCount", totalItemsCount);
        
        // Chart Data (Last 6 months)
        LocalDate startOfSixMonthsAgo = today.minusMonths(5).withDayOfMonth(1);
        LocalDate endOfCurrentMonth = today.withDayOfMonth(today.lengthOfMonth());
        List<TransactionMonthlySummary> last6Months = transactionService.getMonthlySummary(startOfSixMonthsAgo, endOfCurrentMonth);
        
        List<String> chartLabels = new ArrayList<>();
        List<Long> chartSales = new ArrayList<>();
        List<Long> chartPayments = new ArrayList<>();
        
        for (TransactionMonthlySummary summary : last6Months) {
            String monthName = summary.month().getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
            monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
            chartLabels.add("'" + monthName + " " + summary.month().getYear() + "'");
            chartSales.add(summary.salesAmount());
            chartPayments.add(summary.paymentsAmount());
        }
        
        model.addAttribute("chartLabels", String.join(",", chartLabels));
        model.addAttribute("chartSales", chartSales.toString().replaceAll("\\[|\\]", ""));
        model.addAttribute("chartPayments", chartPayments.toString().replaceAll("\\[|\\]", ""));
        
        return "dashboard/index";
    }

    @GetMapping("/birthdays")
    public String birthdays(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("birthdays", customerService.getUpcomingBirthdays(today.getMonthValue(), today.getDayOfMonth()));
        return "dashboard/birthdays";
    }

    @GetMapping("/finished-cards")
    public String finishedCards(Model model) {
        model.addAttribute("transactions", transactionService.getFinishedCardsThisMonth());
        return "dashboard/finished-cards";
    }

    @GetMapping("/sales")
    public String sales(Model model) {
        model.addAttribute("transactions", transactionService.getSalesThisMonth());
        return "dashboard/sales";
    }

}