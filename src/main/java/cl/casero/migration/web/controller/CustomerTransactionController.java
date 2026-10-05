package cl.casero.migration.web.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
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

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.TransactionService;
import cl.casero.migration.service.dto.DebtForgivenessForm;
import cl.casero.migration.service.dto.MoneyTransactionForm;
import cl.casero.migration.service.dto.PaymentForm;
import cl.casero.migration.service.dto.SaleForm;
import cl.casero.migration.util.CurrencyUtil;
import cl.casero.migration.util.DateUtil;
import cl.casero.migration.web.audit.CustomerAuditLogger;
import cl.casero.migration.web.util.CustomerFormRedirect;

@Controller
@RequiredArgsConstructor
@RequestMapping("/customers")
public class CustomerTransactionController {

    private static final int MAX_PAGE_SIZE = 50;

    private final CustomerService customerService;
    private final TransactionService transactionService;
    private final CustomerAuditLogger customerAuditLogger;

    @ResponseBody
    @GetMapping(value = "/{id}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public TransactionPageResponse listTransactions(
        @PathVariable Long id,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size,
        @RequestParam(value = "ascending", defaultValue = "false") boolean ascending
    ) {
        int sanitizedPage = Math.max(page, 0);
        int positiveSize = Math.max(size, 1);
        int sanitizedSize = Math.min(positiveSize, MAX_PAGE_SIZE);
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);
        Page<Transaction> transactions = transactionService.listByCustomer(id, pageable);
        List<Transaction> customerTransactions = transactions.getContent();
        List<TransactionCard> content = customerTransactions.stream()
                .map(this::toTransactionCard)
                .toList();
        int currentPage = transactions.getNumber();
        int totalPages = transactions.getTotalPages();
        long totalElements = transactions.getTotalElements();
        boolean hasPrevious = transactions.hasPrevious();
        boolean hasNext = transactions.hasNext();

        return new TransactionPageResponse(content, currentPage, totalPages, totalElements, hasPrevious, hasNext);
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
            return CustomerFormRedirect.redirectToAction(id, redirectAttributes, "saleForm", form, result, "sale");
        }

        transactionService.registerSale(id, form);
        redirectAttributes.addFlashAttribute("message", "Venta registrada");
        customerAuditLogger.logSale(id, form, authentication, request);

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
            return CustomerFormRedirect.redirectToAction(id, redirectAttributes, "paymentForm", form, result, "payment");
        }

        transactionService.registerPayment(id, form);
        redirectAttributes.addFlashAttribute("message", "Pago registrado");
        customerAuditLogger.logPayment(id, form, authentication, request);

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
            return CustomerFormRedirect.redirectToAction(id, redirectAttributes, "refundForm", form, result, "refund");
        }

        transactionService.registerRefund(id, form);
        redirectAttributes.addFlashAttribute("message", "Devolución registrada");
        customerAuditLogger.logRefund(id, form, authentication, request);

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
            return CustomerFormRedirect.redirectToAction(id, redirectAttributes, "faultDiscountForm", form, result, "fault-discount");
        }

        transactionService.registerFaultDiscount(id, form);
        redirectAttributes.addFlashAttribute("message", "Descuento por falla registrado");
        customerAuditLogger.logFaultDiscount(id, form, authentication, request);

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
            return CustomerFormRedirect.redirectToAction(id, redirectAttributes, "debtForgivenessForm", form, result, "forgiveness");
        }

        transactionService.forgiveDebt(id, form);
        redirectAttributes.addFlashAttribute("message", "Deuda condonada");
        customerAuditLogger.logDebtForgiveness(id, form, authentication, request);

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
        customerAuditLogger.logTransactionDeleted(transactionId, customerId, authentication, request);

        return "redirect:/customers/" + customerId;
    }

    @GetMapping("/{id}/actions/payment")
    public String showPaymentForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        model.addAttribute("customer", customer);

        if (!model.containsAttribute("paymentForm")) {
            PaymentForm form = new PaymentForm();
            LocalDate today = LocalDate.now();
            form.setDate(today);
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
            LocalDate today = LocalDate.now();
            form.setDate(today);
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
            LocalDate today = LocalDate.now();
            form.setDate(today);
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
            LocalDate today = LocalDate.now();
            form.setDate(today);
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
            LocalDate today = LocalDate.now();
            form.setDate(today);
            model.addAttribute("debtForgivenessForm", form);
        }

        return "customers/actions/forgiveness";
    }

    private TransactionCard toTransactionCard(Transaction transaction) {
        Long transactionId = transaction.getId();
        LocalDate date = transaction.getDate();
        String formattedDate = DateUtil.format(date);
        TransactionType type = transaction.getType();
        String typeKey = type.name();
        String detail = transaction.getDetail();
        Integer amount = transaction.getAmount();
        String formattedAmount = CurrencyUtil.format(amount);
        Integer balance = transaction.getBalance();
        String formattedBalance = CurrencyUtil.format(balance);

        return new TransactionCard(transactionId, formattedDate, typeKey, detail, formattedAmount, formattedBalance);
    }

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
