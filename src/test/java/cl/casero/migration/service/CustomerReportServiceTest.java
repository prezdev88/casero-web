package cl.casero.migration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.dto.CustomerTransactionReportData;
import cl.casero.migration.service.dto.ReportCustomerData;
import cl.casero.migration.service.dto.ReportTransactionData;
import cl.casero.migration.util.CurrencyUtil;
import cl.casero.migration.util.TransactionTypeUtil;

class CustomerReportServiceTest {

    private static final int AMOUNT = 100;
    private static final int BALANCE = 200;
    private static final LocalDate MOVEMENT_DATE = LocalDate.parse("2026-10-04");

    private final CustomerReportService renderer = new CustomerReportService();

    @ParameterizedTest
    @EnumSource(TransactionType.class)
    void rendersSnapshotFieldsAndPreservesFinancialSigns(TransactionType type) throws IOException {
        ReportCustomerData customer = new ReportCustomerData("Test Customer", "Test Address", "Test Sector", BALANCE);
        ReportTransactionData movement = new ReportTransactionData(MOVEMENT_DATE, type, "Test Detail", AMOUNT, BALANCE);
        List<ReportTransactionData> movements = List.of(movement);
        CustomerTransactionReportData report = new CustomerTransactionReportData(customer, movements, "Selected range", type);

        String text = renderText(report);

        String label = TransactionTypeUtil.label(type);
        int signedAmount = type.isDebtDecreaser() ? -AMOUNT : AMOUNT;
        String formattedAmount = CurrencyUtil.format(signedAmount);
        String compactAmount = compact(formattedAmount);
        String compactText = compact(text);
        assertThat(text).contains("Test Customer", "Test Address", "Test Sector", "Selected range", "Test Detail", label);
        assertThat(compactText).contains(compactAmount);
        assertThat(text).contains("Generado el", "Movimientos incluidos", "Detalle de transacciones");
    }

    @Test
    void rendersEmptyReportWithoutEntityReferences() throws IOException {
        ReportCustomerData customer = new ReportCustomerData(null, " ", "No asignado", null);
        List<ReportTransactionData> movements = List.of();
        CustomerTransactionReportData report = new CustomerTransactionReportData(customer, movements, null, null);

        String text = renderText(report);

        assertThat(text).contains("No asignado", "Personalizado", "Todos los tipos", "No hay transacciones registradas para este rango.");
    }

    private String renderText(CustomerTransactionReportData report) throws IOException {
        byte[] pdf = renderer.generateTransactionsReport(report);
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            return extractor.getTextFromPage(1);
        } finally {
            reader.close();
        }
    }

    private String compact(String value) {
        return value.replaceAll("[\\s\\u00a0]+", "");
    }
}
