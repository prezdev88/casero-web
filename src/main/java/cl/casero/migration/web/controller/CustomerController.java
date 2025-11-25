package cl.casero.migration.web.controller;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.service.CustomerScoreService;
import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.SectorService;
import cl.casero.migration.service.StatisticsService;
import cl.casero.migration.service.TransactionService;
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
import cl.casero.migration.util.DateUtil;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
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

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerScoreService customerScoreService;
    private final TransactionService transactionService;
    private final StatisticsService statisticsService;
    private final SectorService sectorService;

    public CustomerController(CustomerService customerService,
                              CustomerScoreService customerScoreService,
                              TransactionService transactionService,
                              StatisticsService statisticsService,
                              SectorService sectorService) {
        this.customerService = customerService;
        this.customerScoreService = customerScoreService;
        this.transactionService = transactionService;
        this.statisticsService = statisticsService;
        this.sectorService = sectorService;
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public String listCustomers(@RequestParam(value = "q", required = false) String query,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                Model model) {
        boolean hasQuery = query != null && !query.isBlank();
        Page<Customer> customersPage = searchCustomers(query, page, size);
        model.addAttribute("customerScores", customerScoreService.calculateScores(customersPage.getContent()));
        model.addAttribute("customersPage", customersPage);
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("showResults", hasQuery);
        return "customers/list";
    }

    @GetMapping("/ranking")
    public String ranking(@RequestParam(value = "page", defaultValue = "0") int page,
                          @RequestParam(value = "size", defaultValue = "20") int size,
                          @RequestParam(value = "direction", defaultValue = "desc") String direction,
                          Model model) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 50);
        boolean ascending = "asc".equalsIgnoreCase(direction);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize);
        Page<CustomerScoreService.RankingEntry> rankingPage = customerScoreService.getRanking(pageable, ascending);
        model.addAttribute("rankingPage", rankingPage);
        model.addAttribute("direction", ascending ? "asc" : "desc");
        model.addAttribute("nextDirection", ascending ? "desc" : "asc");
        return "customers/ranking";
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public CustomerPageResponse listCustomersJson(@RequestParam(value = "q", required = false) String query,
                                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                                  @RequestParam(value = "size", defaultValue = "10") int size) {
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
    public String createCustomer(@Valid @ModelAttribute("customerForm") CreateCustomerForm form,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerForm", result);
            redirectAttributes.addFlashAttribute("customerForm", form);
            return "redirect:/customers/new";
        }

        customerService.create(form);
        redirectAttributes.addFlashAttribute("message", "Cliente creado correctamente");
        return "redirect:/customers";
    }

    @GetMapping("/{id}")
    public String viewCustomer(@PathVariable Long id,
                               @RequestParam(value = "ascending", defaultValue = "false") boolean ascending,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               Model model) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 50);
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);
        Customer customer = customerService.get(id);
        model.addAttribute("customerScore", customerScoreService.calculateScore(customer));
        Page<Transaction> transactions = transactionService.listByCustomer(id, pageable);

        model.addAttribute("customer", customer);
        model.addAttribute("transactionsPage", transactions);
        model.addAttribute("ascending", ascending);

        return "customers/detail";
    }

    @GetMapping(value = "/{id}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TransactionPageResponse listTransactions(@PathVariable Long id,
                                                    @RequestParam(value = "page", defaultValue = "0") int page,
                                                    @RequestParam(value = "size", defaultValue = "10") int size,
                                                    @RequestParam(value = "ascending", defaultValue = "false") boolean ascending) {
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

    @PostMapping("/{id}/sales")
    public String registerSale(@PathVariable Long id,
                               @Valid @ModelAttribute("saleForm") SaleForm form,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "saleForm", form, result, "sale");
        }
        transactionService.registerSale(id, form);
        redirectAttributes.addFlashAttribute("message", "Venta registrada");
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/payments")
    public String registerPayment(@PathVariable Long id,
                                  @Valid @ModelAttribute("paymentForm") PaymentForm form,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "paymentForm", form, result, "payment");
        }
        transactionService.registerPayment(id, form);
        redirectAttributes.addFlashAttribute("message", "Pago registrado");
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/refunds")
    public String registerRefund(@PathVariable Long id,
                                 @Valid @ModelAttribute("refundForm") MoneyTransactionForm form,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "refundForm", form, result, "refund");
        }
        transactionService.registerRefund(id, form);
        redirectAttributes.addFlashAttribute("message", "Devolución registrada");
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/fault-discounts")
    public String registerFaultDiscount(@PathVariable Long id,
                                        @Valid @ModelAttribute("faultDiscountForm") MoneyTransactionForm form,
                                        BindingResult result,
                                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "faultDiscountForm", form, result, "fault-discount");
        }
        transactionService.registerFaultDiscount(id, form);
        redirectAttributes.addFlashAttribute("message", "Descuento por falla registrado");
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/forgiveness")
    public String forgiveDebt(@PathVariable Long id,
                              @Valid @ModelAttribute("debtForgivenessForm") DebtForgivenessForm form,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "debtForgivenessForm", form, result, "forgiveness");
        }
        transactionService.forgiveDebt(id, form);
        redirectAttributes.addFlashAttribute("message", "Deuda condonada");
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/address")
    public String updateAddress(@PathVariable Long id,
                                @Valid @ModelAttribute("updateAddressForm") UpdateAddressForm form,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "updateAddressForm", form, result, "address/edit");
        }
        customerService.updateAddress(id, form.getNewAddress());
        redirectAttributes.addFlashAttribute("successMessage", "Dirección actualizada");
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/sector")
    public String updateSector(@PathVariable Long id,
                               @Valid @ModelAttribute("updateSectorForm") UpdateSectorForm form,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "updateSectorForm", form, result, "sector/edit");
        }
        customerService.updateSector(id, form.getSectorId());
        redirectAttributes.addFlashAttribute("successMessage", "Sector actualizado");
        return "redirect:/customers/" + id;
    }

    @PostMapping("/transactions/{transactionId}/delete")
    public String deleteTransaction(@PathVariable Long transactionId,
                                    @RequestParam("customerId") Long customerId,
                                    RedirectAttributes redirectAttributes) {
        transactionService.delete(transactionId);
        redirectAttributes.addFlashAttribute("message", "Transacción eliminada");
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
    public String updateName(@PathVariable Long id,
                             @Valid @ModelAttribute("updateNameForm") UpdateNameForm form,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return redirectToAction(id, redirectAttributes, "updateNameForm", form, result, "name/edit");
        }
        customerService.updateName(id, form.getNewName());
        redirectAttributes.addFlashAttribute("successMessage", "Nombre actualizado");
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

    private String redirectToAction(Long id,
                                    RedirectAttributes redirectAttributes,
                                    String attributeName,
                                    Object form,
                                    BindingResult result,
                                    String actionPath) {
        redirectAttributes.addFlashAttribute(
                "org.springframework.validation.BindingResult." + attributeName, result);
        redirectAttributes.addFlashAttribute(attributeName, form);
        return "redirect:/customers/" + id + "/actions/" + actionPath;
    }

    public record CustomerSearchResult(Long id,
                                       String name,
                                       String formattedDebt,
                                       Integer debtValue,
                                       Double score) {
    }

    public record CustomerPageResponse(List<CustomerSearchResult> content,
                                       int page,
                                       int totalPages,
                                       long totalElements,
                                       boolean hasPrevious,
                                       boolean hasNext) {
    }

    public record TransactionCard(Long id,
                                  String formattedDate,
                                  String typeKey,
                                  String detail,
                                  String formattedAmount,
                                  String formattedBalance) {
    }

    public record TransactionPageResponse(List<TransactionCard> content,
                                          int page,
                                          int totalPages,
                                          long totalElements,
                                          boolean hasPrevious,
                                          boolean hasNext) {
    }
}
