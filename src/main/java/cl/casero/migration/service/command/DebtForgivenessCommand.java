package cl.casero.migration.service.command;

import java.time.LocalDate;

public record DebtForgivenessCommand(LocalDate date, String detail) {}
