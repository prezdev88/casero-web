package cl.casero.migration.web.controller;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Transaction;
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
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final TransactionService transactionService;
    private final StatisticsService statisticsService;
    private final SectorService sectorService;

    public CustomerController(CustomerService customerService,
                              TransactionService transactionService,
                              StatisticsService statisticsService,
                              SectorService sectorService) {
        this.customerService = customerService;
        this.transactionService = transactionService;
        this.statisticsService = statisticsService;
        this.sectorService = sectorService;
    }

    @GetMapping
    public String listCustomers(@RequestParam(value = "q", required = false) String query, Model model) {
        boolean hasQuery = query != null && !query.isBlank();
        List<Customer> customers = hasQuery ? customerService.search(query.trim()) : List.of();
        model.addAttribute("customers", customers);
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("showResults", hasQuery);
        return "customers/list";
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
                               Model model) {
        Customer customer = customerService.get(id);
        List<Transaction> transactions = transactionService.listByCustomer(id, ascending);

        model.addAttribute("customer", customer);
        model.addAttribute("transactions", transactions);
        model.addAttribute("ascending", ascending);

        return "customers/detail";
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
        redirectAttributes.addFlashAttribute("message", "Dirección actualizada");
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
            model.addAttribute("paymentForm", new PaymentForm());
        }
        return "customers/actions/payment";
    }

    @GetMapping("/{id}/actions/sale")
    public String showSaleForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        model.addAttribute("customer", customer);
        if (!model.containsAttribute("saleForm")) {
            model.addAttribute("saleForm", new SaleForm());
        }
        return "customers/actions/sale";
    }

    @GetMapping("/{id}/actions/refund")
    public String showRefundForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        model.addAttribute("customer", customer);
        if (!model.containsAttribute("refundForm")) {
            model.addAttribute("refundForm", new MoneyTransactionForm());
        }
        return "customers/actions/refund";
    }

    @GetMapping("/{id}/actions/forgiveness")
    public String showForgivenessForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        model.addAttribute("customer", customer);
        if (!model.containsAttribute("debtForgivenessForm")) {
            model.addAttribute("debtForgivenessForm", new DebtForgivenessForm());
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
}
