package cl.casero.migration.web.controller;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.CustomerReportService;
import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.TransactionService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/customers")
public class CustomerReportController {

    private static final int DEFAULT_REPORT_MONTHS = 12;
    private static final int MAX_REPORT_MONTHS = 60;

    private final CustomerService customerService;
    private final TransactionService transactionService;
    private final CustomerReportService customerReportService;

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
        String safeName = (customer.getName() != null) ? customer.getName().replaceAll("[^a-zA-Z0-9]+", "-") : "cliente";
        String filename = "casero-informe-" + safeName + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    private TransactionType parseReportType(String raw) {
        try {
            if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) {
                return null;
            }

            String typeName = raw.toUpperCase();
            return TransactionType.valueOf(typeName);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isMonthsRange(String rangeParam) {
        return "MONTHS".equalsIgnoreCase(rangeParam);
    }

    private int sanitizeMonths(Integer monthsParam) {
        if (monthsParam == null || monthsParam < 1) {
            return DEFAULT_REPORT_MONTHS;
        }

        return Math.min(monthsParam, MAX_REPORT_MONTHS);
    }

    private List<Transaction> filterTransactionsByMonths(List<Transaction> transactions, int months) {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        LocalDate reference = today.withDayOfMonth(1);
        LocalDate cutoff = reference.minusMonths(months - 1);
        return transactions.stream()
                .filter(tx -> tx.getDate() != null && !tx.getDate().isBefore(cutoff))
                .toList();
    }
}
