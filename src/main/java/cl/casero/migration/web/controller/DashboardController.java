package cl.casero.migration.web.controller;

import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.StatisticsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final CustomerService customerService;
    private final StatisticsService statisticsService;

    @GetMapping
    public String index(Model model) {
        LocalDate today = LocalDate.now();
        long upcomingBirthdaysCount = customerService.getUpcomingBirthdaysThisMonthCount(today.getMonthValue(), today.getDayOfMonth());
        int totalDebt = statisticsService.getTotalDebt();
        
        model.addAttribute("upcomingBirthdaysCount", upcomingBirthdaysCount);
        model.addAttribute("totalDebt", totalDebt);
        return "dashboard/index";
    }

    @GetMapping("/birthdays")
    public String birthdays(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("birthdays", customerService.getUpcomingBirthdays(today.getMonthValue(), today.getDayOfMonth()));
        return "dashboard/birthdays";
    }
}
