package cl.casero.migration.web.controller;

import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.TransactionService;
import cl.casero.migration.service.dto.TransactionMonthlySummary;
import cl.casero.migration.util.CurrencyUtil;
import cl.casero.migration.util.DateTimeUtil;
import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;

@Controller
@AllArgsConstructor
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public String listTransactions(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size,
        @RequestParam(value = "type", required = false) TransactionType type,
        Model model
    ) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 50);
        Sort sort = Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);
        Page<Transaction> transactionsPage = transactionService.listAll(type, pageable);

        model.addAttribute("transactionsPage", transactionsPage);
        model.addAttribute("dateTimeUtil", DateTimeUtil.class);
        model.addAttribute("currencyUtil", CurrencyUtil.class);
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("selectedType", type);

        return "transactions/list";
    }

    @ResponseBody
    @GetMapping(value = "/monthly-stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TransactionMonthlySummary> monthlyStats(
        @RequestParam(value = "startDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(value = "endDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return transactionService.getMonthlySummary(startDate, endDate);
    }
}
