package cl.casero.migration.web.controller;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import cl.casero.migration.service.CustomerQueries;
import cl.casero.migration.service.DashboardService;
import cl.casero.migration.service.TransactionQueries;
import cl.casero.migration.service.dto.CustomerBirthdayDTO;
import cl.casero.migration.service.dto.DashboardData;
import cl.casero.migration.service.dto.TopCustomerSummary;
import cl.casero.migration.service.dto.TransactionMonthlySummary;
import cl.casero.migration.web.presentation.CustomerBirthdayPresenter;
import cl.casero.migration.web.presentation.CustomerBirthdayView;
import cl.casero.migration.web.presentation.DashboardChartData;
import cl.casero.migration.web.presentation.DashboardChartPresenter;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final CustomerQueries customerQueries;
    private final TransactionQueries transactionQueries;
    private final DashboardService dashboardService;
    private final CustomerBirthdayPresenter birthdayPresenter;
    private final DashboardChartPresenter chartPresenter;

    @GetMapping
    public String index(Model model) {
        LocalDate today = LocalDate.now();
        DashboardData data = dashboardService.prepare(today);
        long birthdaysCount = data.birthdaysCount();
        model.addAttribute("birthdaysCount", birthdaysCount);
        int totalDebt = data.totalDebt();
        model.addAttribute("totalDebt", totalDebt);
        long activeCustomersCount = data.activeCustomersCount();
        model.addAttribute("activeCustomersCount", activeCustomersCount);
        long overdueCustomersCount = data.overdueCustomersCount();
        model.addAttribute("overdueCustomersCount", overdueCustomersCount);
        long overdueDebt = data.overdueDebt();
        model.addAttribute("overdueDebt", overdueDebt);
        long salesAmount = data.salesAmount();
        model.addAttribute("salesAmount", salesAmount);
        long paymentsAmount = data.paymentsAmount();
        model.addAttribute("paymentsAmount", paymentsAmount);
        long lastMonthSales = data.lastMonthSales();
        model.addAttribute("lastMonthSales", lastMonthSales);
        long lastMonthPayments = data.lastMonthPayments();
        model.addAttribute("lastMonthPayments", lastMonthPayments);
        List<TopCustomerSummary> topCustomers = data.topCustomers();
        model.addAttribute("topCustomers", topCustomers);
        int averageDebt = data.averageDebt();
        model.addAttribute("averageDebt", averageDebt);
        int finishedCardsCount = data.finishedCardsCount();
        model.addAttribute("finishedCardsCount", finishedCardsCount);
        int totalItemsCount = data.totalItemsCount();
        model.addAttribute("totalItemsCount", totalItemsCount);
        List<TransactionMonthlySummary> series = data.monthlySeries();
        DashboardChartData chart = chartPresenter.present(series);
        List<String> labels = chart.labels();
        List<Long> sales = chart.sales();
        List<Long> payments = chart.payments();
        model.addAttribute("chartLabels", labels);
        model.addAttribute("chartSales", sales);
        model.addAttribute("chartPayments", payments);
        return "dashboard/index";
    }

    @GetMapping("/birthdays")
    public String birthdays(Model model) {
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        List<CustomerBirthdayDTO> monthlyBirthdays = customerQueries.getBirthdaysThisMonth(currentMonth);
        List<CustomerBirthdayView> birthdays = monthlyBirthdays.stream()
                .map(birthday -> birthdayPresenter.present(birthday, today))
                .toList();
        model.addAttribute("birthdays", birthdays);
        return "dashboard/birthdays";
    }

    @GetMapping("/finished-cards")
    public String finishedCards(Model model) {
        model.addAttribute("transactions", transactionQueries.getFinishedCardsThisMonth());
        return "dashboard/finished-cards";
    }

    @GetMapping("/sales")
    public String sales(Model model) {
        model.addAttribute("transactions", transactionQueries.getSalesThisMonth());
        return "dashboard/sales";
    }

}
