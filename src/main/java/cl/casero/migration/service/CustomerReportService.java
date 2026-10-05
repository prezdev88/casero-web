package cl.casero.migration.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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

import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.dto.CustomerTransactionReportData;
import cl.casero.migration.service.dto.ReportCustomerData;
import cl.casero.migration.service.dto.ReportTransactionData;
import cl.casero.migration.util.CurrencyUtil;
import cl.casero.migration.util.TransactionTypeUtil;

@Service
public class CustomerReportService {

    private static final Locale LOCALE_CL = new Locale("es", "CL");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", LOCALE_CL);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", LOCALE_CL);
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Santiago");
    private static final int PAGE_MARGIN = 36;
    private static final int TOP_MARGIN = 54;
    private static final int TITLE_FONT_SIZE = 16;
    private static final int SECTION_FONT_SIZE = 12;
    private static final int BODY_FONT_SIZE = 10;
    private static final int SMALL_FONT_SIZE = 9;
    private static final int HIGHLIGHT_FONT_SIZE = 11;
    private static final float FULL_WIDTH = 100f;
    private static final float TITLE_SPACING = 18f;
    private static final float SUMMARY_SPACING = 8f;
    private static final float RANGE_SPACING = 10f;
    private static final float SECTION_SPACING = 12f;
    private static final float CELL_PADDING = 6f;
    private static final float DATA_PADDING = 5f;
    private static final float TABLE_SPACING = 4f;
    private static final float SUMMARY_LABEL_WIDTH = 2.5f;
    private static final float SUMMARY_VALUE_WIDTH = 4.5f;
    private static final float DATE_WIDTH = 1.3f;
    private static final float VALUE_WIDTH = 1.1f;
    private static final float DETAIL_WIDTH = 2.6f;
    private static final Color BORDER_COLOR = Color.decode("#e6e6e6");
    private static final Color LABEL_BACKGROUND = Color.decode("#f7f9fd");
    private static final Color DEBT_COLOR = Color.decode("#c62828");
    private static final Color TABLE_HEADER_BACKGROUND = Color.decode("#ebf1fb");
    private static final Color SALE_BACKGROUND = Color.decode("#fff9e5");
    private static final Color SALE_BORDER = Color.decode("#ffd78d");

    public byte[] generateTransactionsReport(CustomerTransactionReportData report) {
        try {
            return renderReport(report);
        } catch (DocumentException exception) {
            throw new IllegalStateException("No se pudo generar el informe en PDF", exception);
        }
    }

    private byte[] renderReport(CustomerTransactionReportData report) throws DocumentException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, PAGE_MARGIN, PAGE_MARGIN, TOP_MARGIN, PAGE_MARGIN);
        PdfWriter.getInstance(document, output);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, TITLE_FONT_SIZE);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, SECTION_FONT_SIZE);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, BODY_FONT_SIZE);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, SMALL_FONT_SIZE);
        HeaderFooter header = buildHeader(smallFont);
        document.setHeader(header);
        document.open();

        Paragraph title = new Paragraph("Transacciones", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(TITLE_SPACING);
        document.add(title);
        ReportCustomerData customer = report.customer();
        PdfPTable summary = buildCustomerSummaryTable(customer, normalFont);
        document.add(summary);
        Paragraph spacer = new Paragraph(" ", normalFont);
        document.add(spacer);
        List<ReportTransactionData> transactions = report.transactions();
        String rangeLabel = report.rangeLabel();
        TransactionType filterType = report.filterType();
        int transactionCount = transactions.size();
        PdfPTable rangeTable = buildRangeSummaryTable(rangeLabel, filterType, transactionCount, normalFont);
        rangeTable.setSpacingAfter(RANGE_SPACING);
        document.add(rangeTable);

        if (transactions.isEmpty()) {
            Paragraph empty = new Paragraph("No hay transacciones registradas para este rango.", normalFont);
            empty.setSpacingBefore(RANGE_SPACING);
            document.add(empty);
        } else {
            Paragraph detailTitle = new Paragraph("Detalle de transacciones", sectionFont);
            detailTitle.setSpacingBefore(SECTION_SPACING);
            detailTitle.setSpacingAfter(CELL_PADDING);
            document.add(detailTitle);
            PdfPTable table = buildTransactionsTable(transactions, normalFont);
            table.setSpacingBefore(TABLE_SPACING);
            document.add(table);
        }

        document.close();
        return output.toByteArray();
    }

    private HeaderFooter buildHeader(Font font) {
        LocalDateTime generatedAt = LocalDateTime.now(DEFAULT_ZONE);
        String timestamp = generatedAt.format(TIMESTAMP_FORMAT);
        Phrase phrase = new Phrase("Generado el " + timestamp, font);
        HeaderFooter header = new HeaderFooter(phrase, false);
        header.setAlignment(Element.ALIGN_RIGHT);
        header.setBorderWidthBottom(0);
        return header;
    }

    private PdfPTable buildCustomerSummaryTable(ReportCustomerData customer, Font font) {
        PdfPTable table = summaryTable();
        table.setSpacingAfter(SUMMARY_SPACING);
        String customerName = customer.name();
        String name = normalizeText(customerName, "—");
        addSummaryRow(table, "Nombre", name, font, false);
        String customerAddress = customer.address();
        String address = normalizeText(customerAddress, "—");
        addSummaryRow(table, "Dirección", address, font, false);
        String sectorName = customer.sectorName();
        String sector = normalizeText(sectorName, "—");
        addSummaryRow(table, "Sector", sector, font, false);
        Integer debt = customer.debt();
        String formattedDebt = formatCurrency(debt);
        addSummaryRow(table, "Deuda actual", formattedDebt, font, true);
        return table;
    }

    private PdfPTable buildRangeSummaryTable(String rangeLabel, TransactionType filterType, int count, Font font) {
        PdfPTable table = summaryTable();
        String range = normalizeText(rangeLabel, "Personalizado");
        addSummaryRow(table, "Rango seleccionado", range, font, false);
        String typeLabel = (filterType == null) ? "Todos los tipos" : TransactionTypeUtil.label(filterType);
        addSummaryRow(table, "Tipo seleccionado", typeLabel, font, false);
        String transactionCount = String.valueOf(count);
        addSummaryRow(table, "Movimientos incluidos", transactionCount, font, false);
        return table;
    }

    private PdfPTable summaryTable() {
        float[] widths = {SUMMARY_LABEL_WIDTH, SUMMARY_VALUE_WIDTH};
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(FULL_WIDTH);
        return table;
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font font, boolean highlightValue) {
        Font labelFont = makeBold(font);
        Phrase labelPhrase = new Phrase(label, labelFont);
        PdfPCell labelCell = new PdfPCell(labelPhrase);
        labelCell.setBorderColor(BORDER_COLOR);
        labelCell.setPadding(CELL_PADDING);
        labelCell.setBackgroundColor(LABEL_BACKGROUND);
        table.addCell(labelCell);
        Font valueFont = highlightValue
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, HIGHLIGHT_FONT_SIZE, DEBT_COLOR)
                : font;
        String normalizedValue = normalizeText(value, "—");
        Phrase valuePhrase = new Phrase(normalizedValue, valueFont);
        PdfPCell valueCell = new PdfPCell(valuePhrase);
        valueCell.setBorderColor(BORDER_COLOR);
        valueCell.setPadding(CELL_PADDING);
        table.addCell(valueCell);
    }

    private PdfPTable buildTransactionsTable(List<ReportTransactionData> transactions, Font font) {
        float[] widths = {DATE_WIDTH, VALUE_WIDTH, DETAIL_WIDTH, VALUE_WIDTH, VALUE_WIDTH};
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(FULL_WIDTH);
        Font headerFont = makeBold(font);
        addHeaderCell(table, "Fecha", headerFont);
        addHeaderCell(table, "Tipo", headerFont);
        addHeaderCell(table, "Detalle", headerFont);
        addHeaderCell(table, "Monto", headerFont);
        addHeaderCell(table, "Saldo", headerFont);

        for (ReportTransactionData transaction : transactions) {
            TransactionType type = transaction.type();
            boolean highlight = type == TransactionType.SALE;
            LocalDate date = transaction.date();
            String formattedDate = (date != null) ? date.format(DATE_FORMAT) : "—";
            addDataCell(table, formattedDate, font, highlight);
            String typeLabel = TransactionTypeUtil.label(type);
            addDataCell(table, typeLabel, font, highlight);
            String detail = transaction.detail();
            String normalizedDetail = normalizeText(detail, "—");
            addDataCell(table, normalizedDetail, font, highlight);
            String amount = formatAmount(transaction);
            addDataCell(table, amount, font, highlight);
            Integer balance = transaction.balance();
            String formattedBalance = formatCurrency(balance);
            addDataCell(table, formattedBalance, font, highlight);
        }

        return table;
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        Phrase phrase = new Phrase(text, font);
        PdfPCell cell = new PdfPCell(phrase);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(CELL_PADDING);
        cell.setBackgroundColor(TABLE_HEADER_BACKGROUND);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, Font font, boolean highlight) {
        Font effectiveFont = highlight ? makeBold(font) : font;
        Phrase phrase = new Phrase(text, effectiveFont);
        PdfPCell cell = new PdfPCell(phrase);
        cell.setPadding(DATA_PADDING);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);

        if (highlight) {
            cell.setBackgroundColor(SALE_BACKGROUND);
            cell.setBorderColor(SALE_BORDER);
        }

        table.addCell(cell);
    }

    private Font makeBold(Font baseFont) {
        Font bold = new Font(baseFont);
        bold.setStyle(Font.BOLD);
        return bold;
    }

    private String formatAmount(ReportTransactionData transaction) {
        Integer transactionAmount = transaction.amount();
        int amount = (transactionAmount != null) ? transactionAmount : 0;
        TransactionType type = transaction.type();

        if (type != null && type.isDebtDecreaser()) {
            int absoluteAmount = Math.abs(amount);
            amount = -absoluteAmount;
        }

        return CurrencyUtil.format(amount);
    }

    private String formatCurrency(Integer amount) {
        int safeAmount = (amount != null) ? amount : 0;
        return CurrencyUtil.format(safeAmount);
    }

    private String normalizeText(String text, String fallback) {
        return (text == null || text.isBlank()) ? fallback : text;
    }
}
