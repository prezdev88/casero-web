package cl.casero.migration.web.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.CustomerScoreService;
import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.TransactionService;
import cl.casero.migration.util.CurrencyUtil;
import cl.casero.migration.util.CustomerScoreCalculator;
import cl.casero.migration.util.CustomerScoreSummary;
import cl.casero.migration.util.TransactionTypeUtil;

@Controller
@RequiredArgsConstructor
@RequestMapping("/customers")
public class CustomerController {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Santiago");
    private static final int MAX_PAGE_SIZE = 50;

    private final CustomerService customerService;
    private final CustomerScoreService customerScoreService;
    private final TransactionService transactionService;

    @GetMapping
    public String listCustomers(
        @RequestParam(value = "q", required = false) String query,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size,
        Model model
    ) {
        boolean hasQuery = query != null && !query.isBlank();
        Page<Customer> customersPage = searchCustomers(query, page, size);

        List<Customer> customers = customersPage.getContent();
        Map<Long, Double> scores = customerScoreService.calculateScores(customers);
        model.addAttribute("customerScores", scores);
        model.addAttribute("customersPage", customersPage);
        model.addAttribute("query", (query == null) ? "" : query);
        model.addAttribute("showResults", hasQuery);
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        model.addAttribute("today", today);

        return "customers/list";
    }

    @ResponseBody
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public CustomerPageResponse listCustomersJson(
        @RequestParam(value = "q", required = false) String query,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Page<Customer> result = searchCustomers(query, page, size);
        List<Customer> customers = result.getContent();
        Map<Long, Double> scores = customerScoreService.calculateScores(customers);
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        List<CustomerSearchResult> content = customers.stream()
                .map(customer -> toSearchResult(customer, scores, today))
                .toList();

        int currentPage = result.getNumber();
        int totalPages = result.getTotalPages();
        long totalElements = result.getTotalElements();
        boolean hasPrevious = result.hasPrevious();
        boolean hasNext = result.hasNext();

        return new CustomerPageResponse(content, currentPage, totalPages, totalElements, hasPrevious, hasNext);
    }

    @GetMapping("/{id}")
    public String viewCustomer(
        @PathVariable Long id,
        @RequestParam(value = "ascending", defaultValue = "false") boolean ascending,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size,
        Model model
    ) {
        int sanitizedPage = Math.max(page, 0);
        int positiveSize = Math.max(size, 1);
        int sanitizedSize = Math.min(positiveSize, MAX_PAGE_SIZE);
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);
        Customer customer = customerService.get(id);
        CustomerScoreService.ScorePresentation scorePresentation = customerScoreService.getScorePresentation(customer);
        double score = scorePresentation.score();
        String explanation = scorePresentation.explanation();
        List<CustomerScoreSummary.CycleScore> cycles = scorePresentation.cycles();
        model.addAttribute("customerScore", score);
        model.addAttribute("customerScoreExplanation", explanation);
        List<CustomerScoreSummary.CycleScore> reversedCycles = new ArrayList<>(cycles);
        Collections.reverse(reversedCycles);
        model.addAttribute("customerScoreCycles", reversedCycles);
        Page<Transaction> transactions = transactionService.listByCustomer(id, pageable);

        model.addAttribute("customer", customer);
        model.addAttribute("transactionsPage", transactions);
        model.addAttribute("ascending", ascending);
        Map<String, String> reportTypeOptions = buildReportTypeOptions();
        model.addAttribute("transactionReportTypeOptions", reportTypeOptions);

        return "customers/detail";
    }

    private Page<Customer> searchCustomers(String query, int page, int size) {
        int sanitizedPage = Math.max(page, 0);
        int positiveSize = Math.max(size, 1);
        int sanitizedSize = Math.min(positiveSize, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize);
        boolean hasQuery = query != null && !query.isBlank();
        if (!hasQuery) {
            return Page.empty(pageable);
        }

        String filter = query.trim();
        return customerService.search(filter, pageable);
    }

    private Map<String, String> buildReportTypeOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("ALL", "Todos los tipos");
        for (TransactionType type : TransactionType.values()) {
            String typeName = type.name();
            String label = TransactionTypeUtil.label(type);
            options.put(typeName, label);
        }

        return options;
    }

    private CustomerSearchResult toSearchResult(
        Customer customer,
        Map<Long, Double> scores,
        LocalDate today
    ) {
        Long customerId = customer.getId();
        String name = customer.getName();
        String address = customer.getAddress();
        String formattedBirthDate = customer.getFormattedBirthDate();
        Integer debt = customer.getDebt();
        String formattedDebt = CurrencyUtil.format(debt);
        double minimumScore = CustomerScoreCalculator.minScore();
        Double score = scores.getOrDefault(customerId, minimumScore);
        boolean birthdayToday = customer.isBirthdayOn(today);
        Integer birthdayAge = customer.getBirthdayAgeOn(today);

        return new CustomerSearchResult(
                customerId,
                name,
                address,
                formattedBirthDate,
                formattedDebt,
                debt,
                score,
                birthdayToday,
                birthdayAge);
    }

    public record CustomerSearchResult(
        Long id,
        String name,
        String address,
        String formattedBirthDate,
        String formattedDebt,
        Integer debtValue,
        Double score,
        boolean birthdayToday,
        Integer birthdayAge
    ) {}

    public record CustomerPageResponse(
        List<CustomerSearchResult> content,
        int page,
        int totalPages,
        long totalElements,
        boolean hasPrevious,
        boolean hasNext
    ) {}
}
