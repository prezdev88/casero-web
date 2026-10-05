package cl.casero.migration.domain;

import java.time.LocalDate;

public record CustomerBirthDate(Integer day, Integer month, Integer year) {

    public boolean isBirthdayOn(LocalDate date) {
        return day != null && month != null
                && day == date.getDayOfMonth()
                && month == date.getMonthValue();
    }

    public Integer getBirthdayAgeOn(LocalDate date) {
        if (year == null || !isBirthdayOn(date)) {
            return null;
        }

        return date.getYear() - year;
    }
}
