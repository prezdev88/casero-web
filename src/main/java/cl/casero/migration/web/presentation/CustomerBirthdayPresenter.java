package cl.casero.migration.web.presentation;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import cl.casero.migration.service.dto.CustomerBirthdayDTO;

@Component
@RequiredArgsConstructor
public class CustomerBirthdayPresenter {

    private final CustomerBirthDateFormatter formatter;

    public CustomerBirthdayView present(CustomerBirthdayDTO birthday, LocalDate referenceDate) {
        Long customerId = birthday.getCustomerId();
        String name = birthday.getName();
        Integer day = birthday.getBirthDay();
        Integer month = birthday.getBirthMonth();
        String label = formatter.formatDayAndMonth(day, month);
        String formattedBirthDate = (label == null) ? "" : label;
        Integer birthYear = birthday.getBirthYear();
        int referenceYear = referenceDate.getYear();
        Integer age = (birthYear == null) ? null : referenceYear - birthYear;
        Integer debt = birthday.getDebt();
        LocalDate lastPaymentDate = birthday.getLastPaymentDate();
        return new CustomerBirthdayView(customerId, name, formattedBirthDate, age, debt, lastPaymentDate);
    }
}
