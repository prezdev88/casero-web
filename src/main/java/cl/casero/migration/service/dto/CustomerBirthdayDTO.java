package cl.casero.migration.service.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CustomerBirthdayDTO {
    private Long customerId;
    private String name;
    private Integer birthDay;
    private Integer birthMonth;
    private Integer birthYear;
    private Integer debt;
    private LocalDate lastPaymentDate;
}
