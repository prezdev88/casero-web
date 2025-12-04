package cl.casero.migration.web.controller;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.service.CustomerReportService;
import cl.casero.migration.service.CustomerScoreService;
import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.SectorService;
import cl.casero.migration.service.StatisticsService;
import cl.casero.migration.service.TransactionService;
import cl.casero.migration.service.AuditEventService;
import cl.casero.migration.service.dto.CreateCustomerForm;
import cl.casero.migration.service.dto.DebtForgivenessForm;
import cl.casero.migration.service.dto.MoneyTransactionForm;
import cl.casero.migration.service.dto.PaymentForm;
import cl.casero.migration.service.dto.SaleForm;
import cl.casero.migration.service.dto.UpdateAddressForm;
import cl.casero.migration.service.dto.UpdateNameForm;
import cl.casero.migration.service.dto.UpdateSectorForm;
import cl.casero.migration.util.CurrencyUtil;
import cl.casero.migration.util.CustomerScoreCalculator;
import cl.casero.migration.util.CustomerScoreSummary;
import cl.casero.migration.util.DateUtil;
import cl.casero.migration.util.TransactionTypeUtil;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import cl.casero.migration.web.security.CaseroUserDetails;

@Controller
@AllArgsConstructor
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerScoreService customerScoreService;
    private final TransactionService transactionService;
    private final StatisticsService statisticsService;
    private final SectorService sectorService;
    private final CustomerReportService customerReportService;
    private final AuditEventService auditEventService;

    @GetMapping
    public String listCustomers(
        @RequestParam(value = "q", required = false) String query,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size,
        Model model
    ) {
        boolean hasQuery = query != null && !query.isBlank();
        Page<Customer> customersPage = searchCustomers(query, page, size);

        model.addAttribute("customerScores", customerScoreService.calculateScores(customersPage.getContent()));
        model.addAttribute("customersPage", customersPage);
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("showResults", hasQuery);

        return "customers/list";
    }

    @GetMapping("/ranking")
    public String ranking(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "100") int size,
        @RequestParam(value = "direction", defaultValue = "desc") String direction,
        Model model
    ) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        boolean ascending = "asc".equalsIgnoreCase(direction);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize);
        Page<CustomerScoreService.RankingEntry> rankingPage = customerScoreService.getRanking(pageable, ascending);

        model.addAttribute("rankingPage", rankingPage);
        model.addAttribute("direction", ascending ? "asc" : "desc");
        model.addAttribute("nextDirection", ascending ? "desc" : "asc");

        return "customers/ranking";
    }

    @ResponseBody
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public CustomerPageResponse listCustomersJson(
        @RequestParam(value = "q", required = false) String query,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Page<Customer> result = searchCustomers(query, page, size);
        Map<Long, Double> scores = customerScoreService.calculateScores(result.getContent());
        List<CustomerSearchResult> content = result.getContent()
                .stream()
                .map(customer -> new CustomerSearchResult(
                        customer.getId(),
                        customer.getName(),
                        CurrencyUtil.format(customer.getDebt()),
                        customer.getDebt(),
                        scores.getOrDefault(customer.getId(), CustomerScoreCalculator.minScore())))
                .toList();
        
        return new CustomerPageResponse(
                content,
                result.getNumber(),
                result.getTotalPages(),
                result.getTotalElements(),
                result.hasPrevious(),
                result.hasNext());
    }

    @GetMapping("/new")
    public String newCustomer(Model model) {
        if (!model.containsAttribute("customerForm")) {
            model.addAttribute("customerForm", new CreateCustomerForm());
        }

        model.addAttribute("sectors", sectorService.listAll());
        model.addAttribute("customersCount", statisticsService.getCustomersCount());

        return "customers/new";
    }

    @PostMapping
    public String createCustomer(
        @Valid @ModelAttribute("customerForm") CreateCustomerForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerForm", result);
            redirectAttributes.addFlashAttribute("customerForm", form);

            return "redirect:/customers/new";
        }

        Customer created = customerService.create(form);
        redirectAttributes.addFlashAttribute("message", "Cliente creado correctamente");
        auditEventService.logEvent(
            AuditEventType.ACTION,
            currentUser(authentication),
            actionPayload("CREATE_CUSTOMER", payload(
                "customerId", created.getId(),
                "name", created.getName(),
                "sectorId", created.getSector().getId(),
                "address", sanitize(created.getAddress())
            )),
            request);

        return "redirect:/customers";
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
        int sanitizedSize = Math.min(Math.max(size, 1), 50);
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);
        Customer customer = customerService.get(id);
        CustomerScoreService.ScorePresentation scorePresentation = customerScoreService.getScorePresentation(customer);
        model.addAttribute("customerScore", scorePresentation.score());
        model.addAttribute("customerScoreExplanation", scorePresentation.explanation());
        List<CustomerScoreSummary.CycleScore> reversedCycles = new ArrayList<>(scorePresentation.cycles());
        Collections.reverse(reversedCycles);
        model.addAttribute("customerScoreCycles", reversedCycles);
        Page<Transaction> transactions = transactionService.listByCustomer(id, pageable);

        model.addAttribute("customer", customer);
        model.addAttribute("transactionsPage", transactions);
        model.addAttribute("ascending", ascending);
        model.addAttribute("transactionReportTypeOptions", buildReportTypeOptions());

        return "customers/detail";
    }

    @ResponseBody
    @GetMapping(value = "/{id}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public TransactionPageResponse listTransactions(
        @PathVariable Long id,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size,
        @RequestParam(value = "ascending", defaultValue = "false") boolean ascending
    ) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 50);
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);
        Page<Transaction> transactions = transactionService.listByCustomer(id, pageable);
        List<TransactionCard> content = transactions.getContent()
                .stream()
                .map(transaction -> new TransactionCard(
                        transaction.getId(),
                        DateUtil.format(transaction.getDate()),
                        transaction.getType().name(),
                        transaction.getDetail(),
                        CurrencyUtil.format(transaction.getAmount()),
                        CurrencyUtil.format(transaction.getBalance())))
                .toList();
        return new TransactionPageResponse(
                content,
                transactions.getNumber(),
                transactions.getTotalPages(),
                transactions.getTotalElements(),
                transactions.hasPrevious(),
                transactions.hasNext()
        );
    }

    @ResponseBody
    @GetMapping(value = "/{id}/reports/transactions", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadTransactionsReport(
        @PathVariable Long id,
        @RequestParam(value = "range", defaultValue = "ALL") String rangeParam,
        @RequestParam(value = "months", required = false) Integer monthsParam,
        @RequestParam(value = "type", defaultValue = "ALL") String typeParam
    ) {
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

    private Map<String, String> buildReportTypeOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("ALL", "Todos los tipos");
        for (TransactionType type : TransactionType.values()) {
            options.put(type.name(), TransactionTypeUtil.label(type));
        }
        return options;
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

    @PostMapping("/{id}/sales")
    public String registerSale(
        @PathVariable Long id,
        @Valid @ModelAttribute("saleForm") SaleForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "saleForm", form, result, "sale");
        }

        transactionService.registerSale(id, form);
        redirectAttributes.addFlashAttribute("message", "Venta registrada");
        auditEventService.logEvent(
            AuditEventType.ACTION,
            currentUser(authentication),
            actionPayload("SALE", payload(
                "customerId", id,
                "amount", form.getAmount(),
                "items", form.getItemsCount(),
                "date", dateToString(form.getDate()),
                "detail", sanitize(form.getDetail())
            )),
            request);

        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/payments")
    public String registerPayment(
        @PathVariable Long id,
        @Valid @ModelAttribute("paymentForm") PaymentForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "paymentForm", form, result, "payment");
        }

        transactionService.registerPayment(id, form);
        redirectAttributes.addFlashAttribute("message", "Pago registrado");
        auditEventService.logEvent(
            AuditEventType.ACTION,
            currentUser(authentication),
            actionPayload("PAYMENT", payload(
                "customerId", id,
                "amount", form.getAmount(),
                "date", dateToString(form.getDate())
            )),
            request);

        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/refunds")
    public String registerRefund(
        @PathVariable Long id,
        @Valid @ModelAttribute("refundForm") MoneyTransactionForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "refundForm", form, result, "refund");
        }

        transactionService.registerRefund(id, form);
        redirectAttributes.addFlashAttribute("message", "Devolución registrada");
        auditEventService.logEvent(
            AuditEventType.ACTION,
            currentUser(authentication),
            actionPayload("REFUND", payload(
                "customerId", id,
                "amount", form.getAmount(),
                "date", dateToString(form.getDate()),
                "detail", sanitize(form.getDetail())
            )),
            request);
        
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/fault-discounts")
    public String registerFaultDiscount(
        @PathVariable Long id,
        @Valid @ModelAttribute("faultDiscountForm") MoneyTransactionForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "faultDiscountForm", form, result, "fault-discount");
        }

        transactionService.registerFaultDiscount(id, form);
        redirectAttributes.addFlashAttribute("message", "Descuento por falla registrado");
        auditEventService.logEvent(
            AuditEventType.ACTION,
            currentUser(authentication),
            actionPayload("FAULT_DISCOUNT", payload(
                "customerId", id,
                "amount", form.getAmount(),
                "date", dateToString(form.getDate()),
                "detail", sanitize(form.getDetail())
            )),
            request);

        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/forgiveness")
    public String forgiveDebt(
        @PathVariable Long id,
        @Valid @ModelAttribute("debtForgivenessForm") DebtForgivenessForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "debtForgivenessForm", form, result, "forgiveness");
        }

        transactionService.forgiveDebt(id, form);
        redirectAttributes.addFlashAttribute("message", "Deuda condonada");
        auditEventService.logEvent(
            AuditEventType.ACTION,
            currentUser(authentication),
            actionPayload("DEBT_FORGIVEN", payload(
                "customerId", id,
                "date", dateToString(form.getDate()),
                "detail", sanitize(form.getDetail())
            )),
            request);

        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/address")
    public String updateAddress(
        @PathVariable Long id,
        @Valid @ModelAttribute("updateAddressForm") UpdateAddressForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "updateAddressForm", form, result, "address/edit");
        }

        customerService.updateAddress(id, form.getNewAddress());
        redirectAttributes.addFlashAttribute("successMessage", "Dirección actualizada");
        auditEventService.logEvent(
            AuditEventType.ACTION,
            currentUser(authentication),
            actionPayload("UPDATE_CUSTOMER_ADDRESS", payload(
                "customerId", id,
                "address", sanitize(form.getNewAddress())
            )),
            request);

        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/sector")
    public String updateSector(
        @PathVariable Long id,
        @Valid @ModelAttribute("updateSectorForm") UpdateSectorForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "updateSectorForm", form, result, "sector/edit");
        }

        customerService.updateSector(id, form.getSectorId());
        redirectAttributes.addFlashAttribute("successMessage", "Sector actualizado");
        auditEventService.logEvent(
            AuditEventType.ACTION,
            currentUser(authentication),
            actionPayload("UPDATE_CUSTOMER_SECTOR", payload(
                "customerId", id,
                "sectorId", form.getSectorId()
            )),
            request);

        return "redirect:/customers/" + id;
    }

    @PostMapping("/transactions/{transactionId}/delete")
    public String deleteTransaction(
        @PathVariable Long transactionId,
        @RequestParam("customerId") Long customerId,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        transactionService.delete(transactionId);
        redirectAttributes.addFlashAttribute("message", "Transacción eliminada");
        auditEventService.logEvent(
            AuditEventType.ACTION,
            currentUser(authentication),
            actionPayload("TRANSACTION_DELETED", payload(
                "transactionId", transactionId,
                "customerId", customerId
            )),
            request);

        return "redirect:/customers/" + customerId;
    }

    @GetMapping("/{id}/actions/payment")
    public String showPaymentForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        model.addAttribute("customer", customer);

        if (!model.containsAttribute("paymentForm")) {
            PaymentForm form = new PaymentForm();
            form.setDate(LocalDate.now());
            model.addAttribute("paymentForm", form);
        }

        return "customers/actions/payment";
    }

    @GetMapping("/{id}/actions/sale")
    public String showSaleForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        model.addAttribute("customer", customer);

        if (!model.containsAttribute("saleForm")) {
            SaleForm form = new SaleForm();
            form.setDate(LocalDate.now());
            model.addAttribute("saleForm", form);
        }

        return "customers/actions/sale";
    }

    @GetMapping("/{id}/actions/refund")
    public String showRefundForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        model.addAttribute("customer", customer);

        if (!model.containsAttribute("refundForm")) {
            MoneyTransactionForm form = new MoneyTransactionForm();
            form.setDate(LocalDate.now());
            model.addAttribute("refundForm", form);
        }

        return "customers/actions/refund";
    }

    @GetMapping("/{id}/actions/fault-discount")
    public String showFaultDiscountForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        model.addAttribute("customer", customer);

        if (!model.containsAttribute("faultDiscountForm")) {
            MoneyTransactionForm form = new MoneyTransactionForm();
            form.setDate(LocalDate.now());
            model.addAttribute("faultDiscountForm", form);
        }

        return "customers/actions/fault-discount";
    }

    @GetMapping("/{id}/actions/forgiveness")
    public String showForgivenessForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        model.addAttribute("customer", customer);

        if (!model.containsAttribute("debtForgivenessForm")) {
            DebtForgivenessForm form = new DebtForgivenessForm();
            form.setDate(LocalDate.now());
            model.addAttribute("debtForgivenessForm", form);
        }

        return "customers/actions/forgiveness";
    }

    @GetMapping("/{id}/actions/address")
    public String viewAddress(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerService.get(id));
        return "customers/actions/address";
    }

    @GetMapping("/{id}/actions/address/edit")
    public String editAddress(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        model.addAttribute("customer", customer);

        if (!model.containsAttribute("updateAddressForm")) {
            model.addAttribute("updateAddressForm", new UpdateAddressForm(customer.getAddress()));
        }

        return "customers/actions/address-edit";
    }

    @PostMapping("/{id}/name")
    public String updateName(
        @PathVariable Long id,
        @Valid @ModelAttribute("updateNameForm") UpdateNameForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "updateNameForm", form, result, "name/edit");
        }

        customerService.updateName(id, form.getNewName());
        redirectAttributes.addFlashAttribute("successMessage", "Nombre actualizado");
        auditEventService.logEvent(
            AuditEventType.ACTION,
            currentUser(authentication),
            payload(
                "action", "UPDATE_CUSTOMER_NAME",
                "customerId", id,
                "name", form.getNewName()
            ),
            request);

        return "redirect:/customers/" + id;
    }

    @GetMapping("/{id}/actions/name/edit")
    public String editName(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        model.addAttribute("customer", customer);

        if (!model.containsAttribute("updateNameForm")) {
            model.addAttribute("updateNameForm", new UpdateNameForm(customer.getName()));
        }

        return "customers/actions/name-edit";
    }

    @GetMapping("/{id}/actions/sector/edit")
    public String editSector(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        model.addAttribute("customer", customer);

        if (!model.containsAttribute("updateSectorForm")) {
            UpdateSectorForm form = new UpdateSectorForm();
            form.setSectorId(customer.getSector().getId());
            model.addAttribute("updateSectorForm", form);
        }

        model.addAttribute("sectors", sectorService.listAll());

        return "customers/actions/sector-edit";
    }

    private Page<Customer> searchCustomers(String query, int page, int size) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize);
        boolean hasQuery = query != null && !query.isBlank();
        return hasQuery ? customerService.search(query.trim(), pageable) : Page.empty(pageable);
    }

    private String redirectToAction(
        Long id,
        RedirectAttributes redirectAttributes,
        String attributeName,
        Object form,
        BindingResult result,
        String actionPath
    ) {
        redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult." + attributeName, result);
        redirectAttributes.addFlashAttribute(attributeName, form);

        return "redirect:/customers/" + id + "/actions/" + actionPath;
    }

    private AppUser currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CaseroUserDetails details) {
            return details.getAppUser();
        }
        return null;
    }

    private Map<String, Object> actionPayload(String type, Map<String, Object> data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("data", data != null ? data : Map.of());
        return payload;
    }

    private Map<String, Object> payload(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        if (kv == null) {
            return map;
        }
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object key = kv[i];
            Object value = kv[i + 1];
            if (key != null && value != null) {
                map.put(String.valueOf(key), value);
            }
        }
        return map;
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String dateToString(java.time.LocalDate date) {
        return date != null ? date.toString() : null;
    }

    public record CustomerSearchResult(
        Long id,
        String name,
        String formattedDebt,
        Integer debtValue,
        Double score
    ) {}

    public record CustomerPageResponse(
        List<CustomerSearchResult> content,
        int page,
        int totalPages,
        long totalElements,
        boolean hasPrevious,
        boolean hasNext
    ) {}

    public record TransactionCard(
        Long id,
        String formattedDate,
        String typeKey,
        String detail,
        String formattedAmount,
        String formattedBalance
    ) {}

    public record TransactionPageResponse(
        List<TransactionCard> content,
        int page,
        int totalPages,
        long totalElements,
        boolean hasPrevious,
        boolean hasNext
    ) {}
}
