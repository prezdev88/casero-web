package cl.casero.migration.web.presentation;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.Year;
import java.util.List;

import org.springframework.stereotype.Component;

import cl.casero.migration.domain.Customer;

@Component
public class CustomerBirthDateFormatter {

    private static final int FIRST_MONTH = Month.JANUARY.getValue();
    private static final int LAST_MONTH = Month.DECEMBER.getValue();
    private static final List<String> MONTH_NAMES = List.of(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre");

    public String format(Customer customer, LocalDate referenceDate) {
        Integer birthDay = customer.getBirthDay();
        Integer birthMonth = customer.getBirthMonth();
        if (birthDay == null || birthMonth == null) {
            return null;
        }

        String monthName = (birthMonth >= FIRST_MONTH && birthMonth <= LAST_MONTH)
                ? MONTH_NAMES.get(birthMonth - 1) : "";
        String label = birthDay + " de " + monthName;
        Integer birthYear = customer.getBirthYear();
        if (birthYear == null) {
            return label;
        }

        int age = calculateAge(customer, referenceDate);
        return label + " de " + birthYear + " (" + age + " años)";
    }

    private int calculateAge(Customer customer, LocalDate referenceDate) {
        int birthYear = customer.getBirthYear();
        int birthMonth = customer.getBirthMonth();
        int birthDay = customer.getBirthDay();
        if (isValidBirthDate(customer)) {
            LocalDate birthDate = LocalDate.of(birthYear, birthMonth, birthDay);
            Period elapsed = Period.between(birthDate, referenceDate);
            return elapsed.getYears();
        }

        int age = referenceDate.getYear() - birthYear;
        int currentMonth = referenceDate.getMonthValue();
        return (currentMonth < birthMonth) ? age - 1 : age;
    }

    private boolean isValidBirthDate(Customer customer) {
        int birthYear = customer.getBirthYear();
        int birthMonth = customer.getBirthMonth();
        if (birthYear < Year.MIN_VALUE || birthYear > Year.MAX_VALUE
                || birthMonth < FIRST_MONTH || birthMonth > LAST_MONTH) {
            return false;
        }

        Month month = Month.of(birthMonth);
        boolean leapYear = Year.isLeap(birthYear);
        int maximumDay = month.length(leapYear);
        int birthDay = customer.getBirthDay();
        return birthDay >= 1 && birthDay <= maximumDay;
    }
}
