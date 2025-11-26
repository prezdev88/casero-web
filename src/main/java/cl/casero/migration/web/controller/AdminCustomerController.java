package cl.casero.migration.web.controller;

import cl.casero.migration.domain.Transaction;
import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final TransactionService transactionService;
    private final CustomerService customerService;

    public AdminCustomerController(TransactionService transactionService,
                                   CustomerService customerService) {
        this.transactionService = transactionService;
        this.customerService = customerService;
    }

    @GetMapping("/{id}/transactions/json")
    @ResponseBody
    public ResponseEntity<List<TransactionExportItem>> exportTransactions(@PathVariable Long id) {
        customerService.get(id);
        List<TransactionExportItem> items = transactionService.listAllByCustomer(id)
                .stream()
                .map(TransactionExportItem::fromEntity)
                .toList();
        return ResponseEntity.ok(items);
    }

    public record TransactionExportItem(Long id,
                                        String date,
                                        String type,
                                        String detail,
                                        Integer amount,
                                        Integer balance) {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

        static TransactionExportItem fromEntity(Transaction transaction) {
            return new TransactionExportItem(
                    transaction.getId(),
                    transaction.getDate() != null ? transaction.getDate().format(FORMATTER) : null,
                    transaction.getType() != null ? transaction.getType().name() : null,
                    transaction.getDetail(),
                    transaction.getAmount(),
                    transaction.getBalance()
            );
        }
    }
}
