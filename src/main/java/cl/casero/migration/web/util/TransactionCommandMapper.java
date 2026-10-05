package cl.casero.migration.web.util;

import java.time.LocalDate;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import cl.casero.migration.service.command.DebtForgivenessCommand;
import cl.casero.migration.service.command.MoneyTransactionCommand;
import cl.casero.migration.service.command.PaymentCommand;
import cl.casero.migration.service.command.SaleCommand;
import cl.casero.migration.web.form.DebtForgivenessForm;
import cl.casero.migration.web.form.MoneyTransactionForm;
import cl.casero.migration.web.form.PaymentForm;
import cl.casero.migration.web.form.SaleForm;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TransactionCommandMapper {

    public static SaleCommand sale(SaleForm form) {
        LocalDate date = form.getDate();
        String detail = form.getDetail();
        Integer itemsCount = form.getItemsCount();
        Integer amount = form.getAmount();
        return new SaleCommand(date, detail, itemsCount, amount);
    }

    public static PaymentCommand payment(PaymentForm form) {
        LocalDate date = form.getDate();
        Integer amount = form.getAmount();
        return new PaymentCommand(date, amount);
    }

    public static MoneyTransactionCommand moneyTransaction(MoneyTransactionForm form) {
        LocalDate date = form.getDate();
        String detail = form.getDetail();
        Integer amount = form.getAmount();
        return new MoneyTransactionCommand(date, detail, amount);
    }

    public static DebtForgivenessCommand debtForgiveness(DebtForgivenessForm form) {
        LocalDate date = form.getDate();
        String detail = form.getDetail();
        return new DebtForgivenessCommand(date, detail);
    }
}
