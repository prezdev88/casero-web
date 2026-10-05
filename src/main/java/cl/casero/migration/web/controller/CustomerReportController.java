package cl.casero.migration.web.controller;

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

import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.CustomerReportService;
import cl.casero.migration.service.CustomerTransactionReportService;
import cl.casero.migration.service.dto.CustomerTransactionReportData;
import cl.casero.migration.service.dto.TransactionReportCriteria;
import cl.casero.migration.service.dto.TransactionReportCriteria.ReportRange;

@Controller
@RequiredArgsConstructor
@RequestMapping("/customers")
public class CustomerReportController {

    private final CustomerTransactionReportService customerTransactionReportService;
    private final CustomerReportService customerReportService;

    @ResponseBody
    @GetMapping(value = "/{id}/reports/transactions", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadTransactionsReport(
        @PathVariable Long id,
        @RequestParam(value = "range", defaultValue = "ALL") String rangeParam,
        @RequestParam(value = "months", required = false) Integer monthsParam,
        @RequestParam(value = "type", defaultValue = "ALL") String typeParam
    ) {
        ReportRange range = parseReportRange(rangeParam);
        TransactionType filterType = parseReportType(typeParam);
        TransactionReportCriteria criteria = new TransactionReportCriteria(range, monthsParam, filterType);
        CustomerTransactionReportData report = customerTransactionReportService.prepare(id, criteria);
        byte[] pdf = customerReportService.generateTransactionsReport(report);
        String customerName = report.customer().name();
        String safeName = (customerName != null) ? customerName.replaceAll("[^a-zA-Z0-9]+", "-") : "cliente";
        String filename = "casero-informe-" + safeName + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    private ReportRange parseReportRange(String raw) {
        return "MONTHS".equalsIgnoreCase(raw) ? ReportRange.MONTHS : ReportRange.ALL;
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

}
