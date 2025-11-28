package cl.casero.migration.service;

import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.dto.DebtForgivenessForm;
import cl.casero.migration.service.dto.MoneyTransactionForm;
import cl.casero.migration.service.dto.PaymentForm;
import cl.casero.migration.service.dto.SaleForm;
import cl.casero.migration.service.dto.TransactionMonthlySummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface TransactionService {
    Page<Transaction> listAll(TransactionType type, Pageable pageable);

    Page<Transaction> listByCustomer(Long customerId, Pageable pageable);

    List<Transaction> listAllByCustomer(Long customerId);

    List<Transaction> listRecentByCustomer(Long customerId, int limit);

    void registerSale(Long customerId, SaleForm form);

    void registerPayment(Long customerId, PaymentForm form);

    void registerRefund(Long customerId, MoneyTransactionForm form);

    void registerFaultDiscount(Long customerId, MoneyTransactionForm form);

    void forgiveDebt(Long customerId, DebtForgivenessForm form);

    void delete(Long transactionId);

    List<TransactionMonthlySummary> getMonthlySummary(LocalDate start, LocalDate end);
}
