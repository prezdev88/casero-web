package cl.casero.migration.api.controller;

import cl.casero.migration.api.dto.CustomerSummaryResponse;
import cl.casero.migration.api.dto.MonthlyStatisticResponse;
import cl.casero.migration.api.dto.PageResponse;
import cl.casero.migration.api.dto.StatisticSummaryResponse;
import cl.casero.migration.domain.Customer;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.CustomerScoreService;
import cl.casero.migration.service.StatisticsService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsApiController {

    private final StatisticsService statisticsService;
    private final CustomerService customerService;
    private final CustomerScoreService customerScoreService;

    public StatisticsApiController(StatisticsService statisticsService,
                                   CustomerService customerService,
                                   CustomerScoreService customerScoreService) {
        this.statisticsService = statisticsService;
        this.customerService = customerService;
        this.customerScoreService = customerScoreService;
    }

    @GetMapping("/summary")
    public StatisticSummaryResponse summary() {
        List<Customer> topDebtors = customerService.getTopDebtors(PageRequest.of(0, 10)).getContent();
        List<Customer> bestCustomers = customerService.getBestCustomers(PageRequest.of(0, 10)).getContent();
        Map<Long, Double> debtorScores = customerScoreService.calculateScores(topDebtors);
        Map<Long, Double> bestScores = customerScoreService.calculateScores(bestCustomers);
        List<CustomerSummaryResponse> topDebtorResponses = topDebtors.stream()
                .map(customer -> toCustomerSummary(customer, debtorScores.get(customer.getId())))
                .toList();
        List<CustomerSummaryResponse> bestCustomerResponses = bestCustomers.stream()
                .map(customer -> toCustomerSummary(customer, bestScores.get(customer.getId())))
                .toList();
        return new StatisticSummaryResponse(
                statisticsService.getTotalDebt(),
                statisticsService.getAverageDebt(),
                statisticsService.getCustomersCount(),
                topDebtorResponses,
                bestCustomerResponses);
    }

    @GetMapping("/monthly")
    public MonthlyStatisticResponse monthly(
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        if (start == null || end == null) {
            LocalDate now = LocalDate.now();
            start = now.withDayOfMonth(1);
            end = start.plusMonths(1).minusDays(1);
        }
        return new MonthlyStatisticResponse(statisticsService.getMonthlyStatistic(start, end), start, end);
    }

    @GetMapping("/debtors")
    public PageResponse<CustomerSummaryResponse> topDebtors(@RequestParam(value = "page", defaultValue = "0") int page,
                                                            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<Customer> debtorsPage = customerService.getTopDebtors(buildPage(page, size, 100));
        Map<Long, Double> scores = customerScoreService.calculateScores(debtorsPage.getContent());
        List<CustomerSummaryResponse> content = debtorsPage.getContent()
                .stream()
                .map(customer -> toCustomerSummary(customer, scores.get(customer.getId())))
                .toList();
        return PageResponse.of(debtorsPage, content);
    }

    @GetMapping("/best-customers")
    public PageResponse<CustomerSummaryResponse> bestCustomers(@RequestParam(value = "page", defaultValue = "0") int page,
                                                               @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<Customer> customersPage = customerService.getBestCustomers(buildPage(page, size, 100));
        Map<Long, Double> scores = customerScoreService.calculateScores(customersPage.getContent());
        List<CustomerSummaryResponse> content = customersPage.getContent()
                .stream()
                .map(customer -> toCustomerSummary(customer, scores.get(customer.getId())))
                .toList();
        return PageResponse.of(customersPage, content);
    }

    @GetMapping("/sectors")
    public PageResponse<CustomerRepository.SectorCountView> customersBySector(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<CustomerRepository.SectorCountView> sectorsPage = customerService.getCustomersCountBySector(buildPage(page, size, 100));
        return PageResponse.of(sectorsPage, sectorsPage.getContent());
    }

    private Pageable buildPage(int page, int size, int maxSize) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), maxSize);
        return PageRequest.of(sanitizedPage, sanitizedSize);
    }

    private CustomerSummaryResponse toCustomerSummary(Customer customer, Double score) {
        return new CustomerSummaryResponse(
                customer.getId(),
                customer.getName(),
                customer.getAddress(),
                customer.getSector() != null ? customer.getSector().getId() : null,
                customer.getSector() != null ? customer.getSector().getName() : null,
                customer.getDebt(),
                score);
    }
}
