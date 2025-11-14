package cl.casero.migration.web.controller;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.MonthlyStatistic;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.StatisticsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
        model.addAttribute("topDebtors", customerService.getTopDebtors(PageRequest.of(0, 10)).getContent());
        model.addAttribute("bestCustomers", customerService.getBestCustomers(PageRequest.of(0, 10)).getContent());
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
    public String topDebtors(@RequestParam(value = "page", defaultValue = "0") int page,
                             @RequestParam(value = "size", defaultValue = "10") int size,
                             Model model) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(sanitizedPage, sanitizedSize);
        Page<Customer> debtorsPage = customerService.getTopDebtors(pageable);
        model.addAttribute("debtorsPage", debtorsPage);
        return "statistics/debtors";
    }

    @GetMapping("/best-customers")
    public String bestCustomers(@RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                Model model) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(sanitizedPage, sanitizedSize);
        Page<Customer> bestCustomersPage = customerService.getBestCustomers(pageable);
        model.addAttribute("customersPage", bestCustomersPage);
        return "statistics/best-customers";
    }

    @GetMapping("/sectors")
    public String customersBySector(@RequestParam(value = "page", defaultValue = "0") int page,
                                    @RequestParam(value = "size", defaultValue = "10") int size,
                                    Model model) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(sanitizedPage, sanitizedSize);
        Page<CustomerRepository.SectorCountView> sectorsPage = customerService.getCustomersCountBySector(pageable);
        model.addAttribute("sectorsPage", sectorsPage);
        return "statistics/sectors";
    }
}
