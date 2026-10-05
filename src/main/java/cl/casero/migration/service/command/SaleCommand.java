package cl.casero.migration.service.command;

import java.time.LocalDate;

public record SaleCommand(LocalDate date, String detail, Integer itemsCount, Integer amount) {}
