package cl.casero.migration.web.controller;

import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.dto.OverdueCustomerSummary;
import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
@RequestMapping("/debtors")
public class DebtorController {

    private final CustomerService customerService;

    @GetMapping
    public String listDebtors(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size,
        @RequestParam(value = "months", required = false) Integer months,
        Model model
    ) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(sanitizedPage, sanitizedSize);
        Page<OverdueCustomerSummary> debtorsPage = Page.empty(pageable);
        boolean hasFilter = months != null;
        Integer appliedMonths = null;

        if (hasFilter) {
            appliedMonths = Math.max(months, 1);
            debtorsPage = customerService.getOverdueCustomers(pageable, appliedMonths);
        }

        model.addAttribute("hasFilter", hasFilter);
        model.addAttribute("months", appliedMonths);
        model.addAttribute("debtorsPage", debtorsPage);
        
        return "customers/debtors";
    }
}
