package cl.casero.migration.web.presentation;

import java.time.LocalDate;

public record CustomerBirthdayView(
    Long customerId, String name, String formattedBirthDate, Integer age,
    Integer debt, LocalDate lastPaymentDate
) {}
