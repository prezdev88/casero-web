package cl.casero.migration.service;

import cl.casero.migration.domain.Transaction;
import cl.casero.migration.service.dto.DebtForgivenessForm;
import cl.casero.migration.service.dto.MoneyTransactionForm;
import cl.casero.migration.service.dto.PaymentForm;
import cl.casero.migration.service.dto.SaleForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {

    Page<Transaction> listAll(Pageable pageable);

    Page<Transaction> listByCustomer(Long customerId, Pageable pageable);

    void registerSale(Long customerId, SaleForm form);

    void registerPayment(Long customerId, PaymentForm form);

    void registerRefund(Long customerId, MoneyTransactionForm form);

    void forgiveDebt(Long customerId, DebtForgivenessForm form);

    void delete(Long transactionId);
}
