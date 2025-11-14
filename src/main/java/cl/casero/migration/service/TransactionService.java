package cl.casero.migration.service;

import cl.casero.migration.domain.Transaction;
import cl.casero.migration.service.dto.DebtForgivenessForm;
import cl.casero.migration.service.dto.MoneyTransactionForm;
import cl.casero.migration.service.dto.PaymentForm;
import cl.casero.migration.service.dto.SaleForm;

import java.util.List;

public interface TransactionService {

    List<Transaction> listByCustomer(Long customerId, boolean ascending);

    void registerSale(Long customerId, SaleForm form);

    void registerPayment(Long customerId, PaymentForm form);

    void registerRefund(Long customerId, MoneyTransactionForm form);

    void forgiveDebt(Long customerId, DebtForgivenessForm form);

    void delete(Long transactionId);
}
