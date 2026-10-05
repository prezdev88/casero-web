package cl.casero.migration.web.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.service.dto.CustomerDetails;
import cl.casero.migration.support.ReadModelFixtures;

class CustomerBirthDateFormatterTest {

    private static final int BIRTH_DAY = 15;
    private static final int BIRTH_MONTH = 10;
    private static final int BIRTH_YEAR = 2000;
    private static final int BIRTHDAY_AGE = 26;
    private static final LocalDate REFERENCE_DATE = LocalDate.parse("2026-10-15");

    private final CustomerBirthDateFormatter formatter = new CustomerBirthDateFormatter();

    @ParameterizedTest
    @CsvSource({
        "1, Enero", "2, Febrero", "3, Marzo", "4, Abril", "5, Mayo", "6, Junio",
        "7, Julio", "8, Agosto", "9, Septiembre", "10, Octubre", "11, Noviembre", "12, Diciembre"
    })
    void preservesMonthNamesAndTheYearlessLabel(int month, String monthName) {
        Customer customer = new Customer();
        customer.setBirthDay(BIRTH_DAY);
        customer.setBirthMonth(month);
        String expected = BIRTH_DAY + " de " + monthName;

        CustomerDetails data = ReadModelFixtures.customer(customer);
        String label = formatter.format(data, REFERENCE_DATE);

        assertThat(label).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "NULL, 10", "15, NULL", "NULL, NULL"
    }, nullValues = "NULL")
    void preservesAbsenceForIncompleteBirthDates(Integer day, Integer month) {
        Customer customer = new Customer();
        customer.setBirthDay(day);
        customer.setBirthMonth(month);
        customer.setBirthYear(BIRTH_YEAR);

        CustomerDetails data = ReadModelFixtures.customer(customer);
        String label = formatter.format(data, REFERENCE_DATE);

        assertThat(label).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "15, 10, 2000, 2026-10-14, 15 de Octubre de 2000 (25 años)",
        "15, 10, 2000, 2026-10-15, 15 de Octubre de 2000 (26 años)",
        "15, 10, 2000, 2026-10-16, 15 de Octubre de 2000 (26 años)",
        "29, 2, 2000, 2025-02-28, 29 de Febrero de 2000 (24 años)",
        "29, 2, 2000, 2025-03-01, 29 de Febrero de 2000 (25 años)",
        "29, 2, 2001, 2026-01-31, 29 de Febrero de 2001 (24 años)",
        "29, 2, 2001, 2026-02-01, 29 de Febrero de 2001 (25 años)",
        "31, 4, 2000, 2026-04-01, 31 de Abril de 2000 (26 años)",
        "15, 13, 2000, 2026-12-31, 15 de  de 2000 (25 años)",
        "15, 0, 2000, 2026-01-01, 15 de  de 2000 (26 años)",
        "0, 10, 2000, 2026-09-30, 0 de Octubre de 2000 (25 años)",
        "15, 10, 2000000000, 2026-10-15, 15 de Octubre de 2000000000 (-1999997974 años)",
        "15, 10, 2030, 2026-10-15, 15 de Octubre de 2030 (-4 años)"
    })
    void preservesCurrentAgeLeapDatesAndTheLegacyFallbackForInvalidDates(
        int day, int month, int year, LocalDate referenceDate, String expected
    ) {
        Customer customer = new Customer();
        customer.setBirthDay(day);
        customer.setBirthMonth(month);
        customer.setBirthYear(year);

        CustomerDetails data = ReadModelFixtures.customer(customer);
        String label = formatter.format(data, referenceDate);

        assertThat(label).isEqualTo(expected);
    }

    @Test
    void keepsDomainBirthdayQueriesExplicitAndIndependentOfTheFormattedCurrentAge() {
        Customer customer = new Customer();
        customer.setBirthDay(BIRTH_DAY);
        customer.setBirthMonth(BIRTH_MONTH);
        customer.setBirthYear(BIRTH_YEAR);
        LocalDate previousDay = REFERENCE_DATE.minusDays(1);

        boolean birthday = customer.isBirthdayOn(REFERENCE_DATE);
        Integer birthdayAge = customer.getBirthdayAgeOn(REFERENCE_DATE);
        boolean previousDayBirthday = customer.isBirthdayOn(previousDay);
        Integer previousDayBirthdayAge = customer.getBirthdayAgeOn(previousDay);
        CustomerDetails data = ReadModelFixtures.customer(customer);
        String previousDayLabel = formatter.format(data, previousDay);

        assertThat(birthday).isTrue();
        assertThat(birthdayAge).isEqualTo(BIRTHDAY_AGE);
        assertThat(previousDayBirthday).isFalse();
        assertThat(previousDayBirthdayAge).isNull();
        assertThat(previousDayLabel).isEqualTo("15 de Octubre de 2000 (25 años)");
    }
}
