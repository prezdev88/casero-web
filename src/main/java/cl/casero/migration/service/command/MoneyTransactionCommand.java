package cl.casero.migration.service.command;

import java.time.LocalDate;

public record MoneyTransactionCommand(LocalDate date, String detail, Integer amount) {}
