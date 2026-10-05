package cl.casero.migration.web.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cl.casero.migration.service.dto.CustomerBirthdayDTO;

class CustomerBirthdayPresenterTest {

    private static final long CUSTOMER_ID = 7L;
    private static final int DEBT = 300;
    private static final LocalDate REFERENCE_DATE = LocalDate.parse("2026-01-01");
    private static final LocalDate LAST_PAYMENT = LocalDate.parse("2025-12-31");
    private static final CustomerBirthDateFormatter FORMATTER = new CustomerBirthDateFormatter();

    @ParameterizedTest
    @CsvSource(value = {
        "31, 12, 2000, 31 de Diciembre, 26",
        "31, 12, NULL, 31 de Diciembre, NULL",
        "NULL, 12, 2000, '', 26",
        "31, NULL, 2000, '', 26",
        "31, 13, 2000, '31 de ', 26"
    }, nullValues = "NULL")
    void preservesTheAgeTurnedInTheReferenceYearAndAllTransportedData(
        Integer day, Integer month, Integer year, String label, Integer age
    ) {
        CustomerBirthdayDTO birthday = new CustomerBirthdayDTO(
                CUSTOMER_ID, "<Name>", day, month, year, DEBT, LAST_PAYMENT);
        CustomerBirthdayPresenter presenter = new CustomerBirthdayPresenter(FORMATTER);

        CustomerBirthdayView result = presenter.present(birthday, REFERENCE_DATE);

        CustomerBirthdayView expected = new CustomerBirthdayView(CUSTOMER_ID, "<Name>", label, age, DEBT, LAST_PAYMENT);
        assertThat(result).isEqualTo(expected);
    }
}
