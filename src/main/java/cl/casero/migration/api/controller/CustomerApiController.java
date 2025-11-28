package cl.casero.migration.api.controller;

import cl.casero.migration.api.dto.CustomerDetailResponse;
import cl.casero.migration.api.dto.CustomerScoreCycleResponse;
import cl.casero.migration.api.dto.CustomerScoreResponse;
import cl.casero.migration.api.dto.CustomerSummaryResponse;
import cl.casero.migration.api.dto.OverdueCustomerResponse;
import cl.casero.migration.api.dto.PageResponse;
import cl.casero.migration.api.dto.RankingEntryResponse;
import cl.casero.migration.api.dto.ScoreResultResponse;
import cl.casero.migration.api.dto.TransactionResponse;
import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.CustomerReportService;
import cl.casero.migration.service.CustomerScoreService;
import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.TransactionService;
import cl.casero.migration.service.dto.CreateCustomerForm;
import cl.casero.migration.service.dto.DebtForgivenessForm;
import cl.casero.migration.service.dto.MoneyTransactionForm;
import cl.casero.migration.service.dto.OverdueCustomerSummary;
import cl.casero.migration.service.dto.PaymentForm;
import cl.casero.migration.service.dto.SaleForm;
import cl.casero.migration.service.dto.UpdateAddressForm;
import cl.casero.migration.service.dto.UpdateNameForm;
import cl.casero.migration.service.dto.UpdateSectorForm;
import cl.casero.migration.util.CustomerScoreCalculator;
import cl.casero.migration.util.CustomerScoreSummary;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerApiController {

    private final CustomerService customerService;
    private final CustomerScoreService customerScoreService;
    private final TransactionService transactionService;
    private final CustomerReportService customerReportService;

    public CustomerApiController(CustomerService customerService,
                                 CustomerScoreService customerScoreService,
                                 TransactionService transactionService,
                                 CustomerReportService customerReportService) {
        this.customerService = customerService;
        this.customerScoreService = customerScoreService;
        this.transactionService = transactionService;
        this.customerReportService = customerReportService;
    }

    @GetMapping
    public PageResponse<CustomerSummaryResponse> listCustomers(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<Customer> customersPage = searchCustomers(query, page, size);
        Map<Long, Double> scores = customerScoreService.calculateScores(customersPage.getContent());
        List<CustomerSummaryResponse> content = customersPage.getContent()
                .stream()
                .map(customer -> toCustomerSummary(customer,
                        scores.getOrDefault(customer.getId(), CustomerScoreCalculator.minScore())))
                .toList();
        return PageResponse.of(customersPage, content);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerSummaryResponse createCustomer(@Valid @RequestBody CreateCustomerForm form) {
        Customer created = customerService.create(form);
        double score = customerScoreService.calculateScore(created);
        return toCustomerSummary(created, score);
    }

    @GetMapping("/{id}")
    public CustomerDetailResponse getCustomer(@PathVariable Long id) {
        Customer customer = customerService.get(id);
        CustomerScoreService.ScorePresentation scorePresentation = customerScoreService.getScorePresentation(customer);
        List<TransactionResponse> recentTransactions = transactionService.listRecentByCustomer(id, 10)
                .stream()
                .map(this::toTransactionResponse)
                .toList();
        return new CustomerDetailResponse(
                toCustomerSummary(customer, scorePresentation.score()),
                toScoreResponse(scorePresentation),
                recentTransactions);
    }

    @GetMapping("/{id}/transactions")
    public PageResponse<TransactionResponse> listTransactions(@PathVariable Long id,
                                                              @RequestParam(value = "page", defaultValue = "0") int page,
                                                              @RequestParam(value = "size", defaultValue = "10") int size,
                                                              @RequestParam(value = "ascending", defaultValue = "false") boolean ascending) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 50);
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);
        Page<Transaction> transactions = transactionService.listByCustomer(id, pageable);
        List<TransactionResponse> content = transactions.getContent()
                .stream()
                .map(this::toTransactionResponse)
                .toList();
        return PageResponse.of(transactions, content);
    }

    @GetMapping(value = "/{id}/transactions/report", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadTransactionsReport(@PathVariable Long id,
                                                             @RequestParam(value = "range", defaultValue = "ALL")
                                                             String rangeParam,
                                                             @RequestParam(value = "months", required = false)
                                                             Integer monthsParam,
                                                             @RequestParam(value = "type", defaultValue = "ALL")
                                                             String typeParam) {
        Customer customer = customerService.get(id);
        TransactionType filterType = parseReportType(typeParam);
        List<Transaction> transactions = transactionService.listAllByCustomer(id);
        String rangeLabel;
        if (isMonthsRange(rangeParam)) {
            int months = sanitizeMonths(monthsParam);
            transactions = filterTransactionsByMonths(transactions, months);
            rangeLabel = "Últimos " + months + (months == 1 ? " mes" : " meses");
        } else {
            rangeLabel = "Todas las transacciones";
        }
        if (filterType != null) {
            transactions = transactions.stream()
                    .filter(tx -> tx.getType() == filterType)
                    .toList();
        }
        byte[] pdf = customerReportService.generateTransactionsReport(customer, transactions, rangeLabel, filterType);
        String safeName = customer.getName() != null ? customer.getName().replaceAll("[^a-zA-Z0-9]+", "-") : "cliente";
        String filename = "casero-informe-" + safeName + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @PostMapping("/{id}/sales")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerSummaryResponse registerSale(@PathVariable Long id,
                                                @Valid @RequestBody SaleForm form) {
        transactionService.registerSale(id, form);
        return summarizeCustomer(id);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerSummaryResponse registerPayment(@PathVariable Long id,
                                                   @Valid @RequestBody PaymentForm form) {
        transactionService.registerPayment(id, form);
        return summarizeCustomer(id);
    }

    @PostMapping("/{id}/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerSummaryResponse registerRefund(@PathVariable Long id,
                                                  @Valid @RequestBody MoneyTransactionForm form) {
        transactionService.registerRefund(id, form);
        return summarizeCustomer(id);
    }

    @PostMapping("/{id}/fault-discounts")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerSummaryResponse registerFaultDiscount(@PathVariable Long id,
                                                         @Valid @RequestBody MoneyTransactionForm form) {
        transactionService.registerFaultDiscount(id, form);
        return summarizeCustomer(id);
    }

    @PostMapping("/{id}/forgiveness")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerSummaryResponse forgiveDebt(@PathVariable Long id,
                                               @Valid @RequestBody DebtForgivenessForm form) {
        transactionService.forgiveDebt(id, form);
        return summarizeCustomer(id);
    }

    @PostMapping("/{id}/name")
    public CustomerSummaryResponse updateName(@PathVariable Long id,
                                              @Valid @RequestBody UpdateNameForm form) {
        customerService.updateName(id, form.getNewName());
        return summarizeCustomer(id);
    }

    @PostMapping("/{id}/address")
    public CustomerSummaryResponse updateAddress(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateAddressForm form) {
        customerService.updateAddress(id, form.getNewAddress());
        return summarizeCustomer(id);
    }

    @PostMapping("/{id}/sector")
    public CustomerSummaryResponse updateSector(@PathVariable Long id,
                                                @Valid @RequestBody UpdateSectorForm form) {
        customerService.updateSector(id, form.getSectorId());
        return summarizeCustomer(id);
    }

    @DeleteMapping("/transactions/{transactionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable Long transactionId) {
        transactionService.delete(transactionId);
    }

    @GetMapping("/ranking")
    public PageResponse<RankingEntryResponse> ranking(@RequestParam(value = "page", defaultValue = "0") int page,
                                                      @RequestParam(value = "size", defaultValue = "100") int size,
                                                      @RequestParam(value = "direction", defaultValue = "desc") String direction) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        boolean ascending = "asc".equalsIgnoreCase(direction);
        PageRequest pageable = PageRequest.of(sanitizedPage, sanitizedSize);
        Page<CustomerScoreService.RankingEntry> rankingPage = customerScoreService.getRanking(pageable, ascending);
        List<RankingEntryResponse> content = rankingPage.getContent()
                .stream()
                .map(entry -> new RankingEntryResponse(entry.getId(),
                        entry.getName(),
                        entry.getDebt(),
                        entry.getScore(),
                        entry.getExplanation(),
                        entry.getCycleCount()))
                .toList();
        return PageResponse.of(rankingPage, content);
    }

    @GetMapping("/overdue")
    public PageResponse<OverdueCustomerResponse> overdue(@RequestParam(value = "months", defaultValue = "1") int months,
                                                         @RequestParam(value = "page", defaultValue = "0") int page,
                                                         @RequestParam(value = "size", defaultValue = "10") int size) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(sanitizedPage, sanitizedSize);
        Page<OverdueCustomerSummary> debtorsPage = customerService.getOverdueCustomers(pageable, Math.max(months, 1));
        List<OverdueCustomerResponse> content = debtorsPage.getContent()
                .stream()
                .map(summary -> new OverdueCustomerResponse(
                        summary.id(),
                        summary.name(),
                        summary.sector(),
                        summary.debt(),
                        summary.lastPayment(),
                        summary.monthsOverdue()))
                .toList();
        return PageResponse.of(debtorsPage, content);
    }

    private CustomerSummaryResponse summarizeCustomer(Long id) {
        Customer customer = customerService.get(id);
        double score = customerScoreService.calculateScore(customer);
        return toCustomerSummary(customer, score);
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

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCustomer() != null ? transaction.getCustomer().getId() : null,
                transaction.getDate(),
                transaction.getDetail(),
                transaction.getAmount(),
                transaction.getBalance(),
                transaction.getType(),
                transaction.getCreatedAt(),
                transaction.getItemCount());
    }

    private CustomerScoreResponse toScoreResponse(CustomerScoreService.ScorePresentation presentation) {
        List<CustomerScoreCycleResponse> cycles = new ArrayList<>();
        for (CustomerScoreSummary.CycleScore cycle : presentation.cycles()) {
            cycles.add(new CustomerScoreCycleResponse(
                    cycle.cycleNumber(),
                    cycle.cycleStartDate(),
                    cycle.cycleEndDate(),
                    toScoreResultResponse(cycle.result())));
        }
        return new CustomerScoreResponse(presentation.score(), presentation.explanation(), cycles);
    }

    private ScoreResultResponse toScoreResultResponse(CustomerScoreCalculator.ScoreResult result) {
        return new ScoreResultResponse(
                result.score(),
                result.daysSinceLastPayment(),
                result.maxIntervalBetweenPayments(),
                result.averageIntervalBetweenPayments(),
                result.lateIntervals(),
                result.totalIntervals(),
                result.hasPayments(),
                result.latestScoreComponent(),
                result.averageScoreComponent(),
                result.coverageScoreComponent(),
                result.historyFactor(),
                result.hasOutstandingDebt(),
                result.paymentMonths(),
                result.cycleMonths());
    }

    private Page<Customer> searchCustomers(String query, int page, int size) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize);
        boolean hasQuery = query != null && !query.isBlank();
        return hasQuery ? customerService.search(query.trim(), pageable) : Page.empty(pageable);
    }

    private TransactionType parseReportType(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) {
            return null;
        }
        try {
            return TransactionType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isMonthsRange(String rangeParam) {
        return "MONTHS".equalsIgnoreCase(rangeParam);
    }

    private int sanitizeMonths(Integer monthsParam) {
        if (monthsParam == null || monthsParam < 1) {
            return 12;
        }
        return Math.min(monthsParam, 60);
    }

    private List<Transaction> filterTransactionsByMonths(List<Transaction> transactions, int months) {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }
        LocalDate reference = LocalDate.now().withDayOfMonth(1);
        LocalDate cutoff = reference.minusMonths(months - 1);
        return transactions.stream()
                .filter(tx -> tx.getDate() != null && !tx.getDate().isBefore(cutoff))
                .toList();
    }
}
