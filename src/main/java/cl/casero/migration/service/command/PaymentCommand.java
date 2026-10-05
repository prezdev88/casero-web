package cl.casero.migration.service.command;

import java.time.LocalDate;

public record PaymentCommand(LocalDate date, Integer amount) {}
