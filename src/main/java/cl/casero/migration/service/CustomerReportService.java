package cl.casero.migration.service;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.util.CurrencyUtil;
import cl.casero.migration.util.TransactionTypeUtil;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class CustomerReportService {

    private static final Locale LOCALE_CL = new Locale("es", "CL");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", LOCALE_CL);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", LOCALE_CL);
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Santiago");

    public byte[] generateTransactionsReport(Customer customer,
                                             List<Transaction> transactions,
                                             String rangeLabel,
                                             TransactionType filterType) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(document, out);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            HeaderFooter header = new HeaderFooter(new Phrase(
                    "Generado el " + LocalDateTime.now(DEFAULT_ZONE).format(TIMESTAMP_FORMAT), smallFont), false);
            header.setAlignment(Element.ALIGN_RIGHT);
            header.setBorderWidthBottom(0);
            document.setHeader(header);
            document.open();

            Paragraph title = new Paragraph("Transacciones", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(18f);
            document.add(title);

            document.add(buildCustomerSummaryTable(customer, normalFont));
            document.add(new Paragraph(" ", normalFont));

            PdfPTable rangeTable = buildRangeSummaryTable(rangeLabel, filterType, transactions.size(), normalFont);
            rangeTable.setSpacingAfter(10f);
            document.add(rangeTable);

            if (transactions.isEmpty()) {
                Paragraph empty = new Paragraph("No hay transacciones registradas para este rango.", normalFont);
                empty.setSpacingBefore(10f);
                document.add(empty);
            } else {
                Paragraph detailTitle = new Paragraph("Detalle de transacciones", sectionFont);
                detailTitle.setSpacingBefore(12f);
                detailTitle.setSpacingAfter(6f);
                document.add(detailTitle);
                PdfPTable table = buildTransactionsTable(transactions, normalFont);
                table.setSpacingBefore(4f);
                document.add(table);
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el informe en PDF", e);
        }
    }

    private static PdfPTable buildRangeSummaryTable(String rangeLabel,
                                                    TransactionType filterType,
                                                    int transactionCount,
                                                    Font font) {
        PdfPTable table = new PdfPTable(new float[]{2.5f, 4.5f});
        table.setWidthPercentage(100f);
        addSummaryRow(table, "Rango seleccionado", normalizeText(rangeLabel, "Personalizado"), font, false);
        String typeLabel = filterType == null ? "Todos los tipos" : TransactionTypeUtil.label(filterType);
        addSummaryRow(table, "Tipo seleccionado", typeLabel, font, false);
        addSummaryRow(table, "Movimientos incluidos", String.valueOf(transactionCount), font, false);
        return table;
    }

    private static PdfPTable buildCustomerSummaryTable(Customer customer, Font font) {
        PdfPTable table = new PdfPTable(new float[]{2.5f, 4.5f});
        table.setWidthPercentage(100f);
        table.setSpacingAfter(8f);
        addSummaryRow(table, "Nombre", safe(customer.getName()), font, false);
        addSummaryRow(table, "Dirección", safe(customer.getAddress()), font, false);
        String sectorName = customer.getSector() != null ? safe(customer.getSector().getName()) : "No asignado";
        addSummaryRow(table, "Sector", sectorName, font, false);
        addSummaryRow(table, "Deuda actual", formatCurrency(customer.getDebt()), font, true);
        return table;
    }

    private static void addSummaryRow(PdfPTable table,
                                      String label,
                                      String value,
                                      Font font,
                                      boolean highlightValue) {
        Font labelFont = new Font(font);
        labelFont.setStyle(Font.BOLD);
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorderColor(new Color(230, 230, 230));
        labelCell.setPadding(6f);
        labelCell.setBackgroundColor(new Color(247, 249, 253));
        table.addCell(labelCell);

        Font valueFont = highlightValue
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(198, 40, 40))
                : font;
        PdfPCell valueCell = new PdfPCell(new Phrase(normalizeText(value, "—"), valueFont));
        valueCell.setBorderColor(new Color(230, 230, 230));
        valueCell.setPadding(6f);
        table.addCell(valueCell);
    }

    private static String normalizeText(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text;
    }

    private static PdfPTable buildTransactionsTable(List<Transaction> transactions, Font font) {
        PdfPTable table = new PdfPTable(new float[]{1.3f, 1.1f, 2.6f, 1.1f, 1.1f});
        table.setWidthPercentage(100f);
        Font headerFont = new Font(font);
        headerFont.setStyle(Font.BOLD);

        addHeaderCell(table, "Fecha", headerFont);
        addHeaderCell(table, "Tipo", headerFont);
        addHeaderCell(table, "Detalle", headerFont);
        addHeaderCell(table, "Monto", headerFont);
        addHeaderCell(table, "Saldo", headerFont);

        for (Transaction transaction : transactions) {
            boolean isSale = transaction.getType() == TransactionType.SALE;
            table.addCell(buildCell(transaction.getDate() != null
                    ? transaction.getDate().format(DATE_FORMAT)
                    : "—", font, isSale));
            table.addCell(buildCell(formatType(transaction.getType()), font, isSale));
            table.addCell(buildCell(safe(transaction.getDetail()), font, isSale));
            table.addCell(buildCell(formatAmount(transaction), font, isSale));
            table.addCell(buildCell(formatCurrency(transaction.getBalance()), font, isSale));
        }

        return table;
    }

    private static void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(6f);
        cell.setBackgroundColor(new Color(235, 241, 251));
        table.addCell(cell);
    }

    private static PdfPCell buildCell(String text, Font font) {
        return buildCell(text, font, false);
    }

    private static PdfPCell buildCell(String text, Font font, boolean highlight) {
        Font effectiveFont = highlight ? makeBold(font) : font;
        PdfPCell cell = new PdfPCell(new Phrase(text, effectiveFont));
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        if (highlight) {
            cell.setBackgroundColor(new Color(255, 249, 229));
            cell.setBorderColor(new Color(255, 215, 141));
        }
        return cell;
    }

    private static Font makeBold(Font baseFont) {
        Font bold = new Font(baseFont);
        bold.setStyle(Font.BOLD);
        return bold;
    }

    private static String formatType(TransactionType type) {
        return TransactionTypeUtil.label(type);
    }

    private static String formatCurrency(Integer amount) {
        int safeAmount = amount != null ? amount : 0;
        return CurrencyUtil.format(safeAmount);
    }

    private static String formatAmount(Transaction transaction) {
        int amount = transaction.getAmount() != null ? transaction.getAmount() : 0;
        if (transaction.getType() != null && transaction.getType().isDebtDecreaser()) {
            amount = -Math.abs(amount);
        }
        return CurrencyUtil.format(amount);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
