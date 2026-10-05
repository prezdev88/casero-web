package cl.casero.migration.web.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.CustomerQueries;
import cl.casero.migration.service.TransactionQueries;
import cl.casero.migration.service.dto.TransactionDetails;

@Controller
@AllArgsConstructor
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final CustomerQueries customerQueries;
    private final TransactionQueries transactionQueries;

    @ResponseBody
    @GetMapping("/{id}/transactions/json")
    public ResponseEntity<List<TransactionExportItem>> exportTransactions(@PathVariable Long id) {
        customerQueries.get(id);

        List<TransactionExportItem> items = transactionQueries.listAllByCustomer(id)
                .stream()
                .map(TransactionExportItem::fromData)
                .toList();

        return ResponseEntity.ok(items);
    }

    public record TransactionExportItem(
        Long id,
        String date,
        String type,
        String detail,
        Integer amount,
        Integer balance
    ) {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

        static TransactionExportItem fromData(TransactionDetails transaction) {
            Long id = transaction.getId();
            LocalDate date = transaction.getDate();
            String formattedDate = (date != null) ? date.format(FORMATTER) : null;
            TransactionType type = transaction.getType();
            String typeName = (type != null) ? type.name() : null;
            String detail = transaction.getDetail();
            Integer amount = transaction.getAmount();
            Integer balance = transaction.getBalance();
            return new TransactionExportItem(id, formattedDate, typeName, detail, amount, balance);
        }
    }
}
