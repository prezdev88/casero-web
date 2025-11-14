package cl.casero.migration.web.controller;

import cl.casero.migration.domain.MonthlyStatistic;
import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final CustomerService customerService;

    public StatisticsController(StatisticsService statisticsService,
                                CustomerService customerService) {
        this.statisticsService = statisticsService;
        this.customerService = customerService;
    }

    @GetMapping
    public String summary(Model model) {
        model.addAttribute("totalDebt", statisticsService.getTotalDebt());
        model.addAttribute("averageDebt", statisticsService.getAverageDebt());
        model.addAttribute("customersCount", statisticsService.getCustomersCount());
        model.addAttribute("topDebtors", customerService.getTopDebtors(10));
        model.addAttribute("bestCustomers", customerService.getBestCustomers(10));
        return "statistics/summary";
    }

    @GetMapping("/monthly")
    public String monthly(
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Model model) {

        if (start == null || end == null) {
            LocalDate now = LocalDate.now();
            start = now.withDayOfMonth(1);
            end = start.plusMonths(1).minusDays(1);
        }

        MonthlyStatistic stats = statisticsService.getMonthlyStatistic(start, end);
        model.addAttribute("stats", stats);
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        return "statistics/monthly";
    }

    @GetMapping("/debtors")
    public String topDebtors(Model model) {
        model.addAttribute("debtors", customerService.getTopDebtors(20));
        return "statistics/debtors";
    }

    @GetMapping("/best-customers")
    public String bestCustomers(Model model) {
        model.addAttribute("customers", customerService.getBestCustomers(20));
        return "statistics/best-customers";
    }

    @GetMapping("/sectors")
    public String customersBySector(Model model) {
        model.addAttribute("sectors", customerService.getCustomersCountBySector());
        return "statistics/sectors";
    }
}
