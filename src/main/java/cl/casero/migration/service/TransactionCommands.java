package cl.casero.migration.service;

import cl.casero.migration.service.command.DebtForgivenessCommand;
import cl.casero.migration.service.command.MoneyTransactionCommand;
import cl.casero.migration.service.command.PaymentCommand;
import cl.casero.migration.service.command.SaleCommand;

public interface TransactionCommands {

    void registerSale(Long customerId, SaleCommand command);

    void registerPayment(Long customerId, PaymentCommand command);

    void registerRefund(Long customerId, MoneyTransactionCommand command);

    void registerFaultDiscount(Long customerId, MoneyTransactionCommand command);

    void forgiveDebt(Long customerId, DebtForgivenessCommand command);

    void delete(Long transactionId);
}
