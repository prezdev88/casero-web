package cl.casero.migration.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class CustomerBirthdayDTO {
    private String name;
    private Integer birthDay;
    private Integer birthMonth;
    private Integer birthYear;
    private Integer debt;
    private LocalDate lastPaymentDate;
    
    public String getFormattedBirthDate() {
        if (birthDay == null || birthMonth == null) return "";
        String[] months = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        String monthName = (birthMonth >= 1 && birthMonth <= 12) ? months[birthMonth - 1] : "";
        return birthDay + " de " + monthName;
    }
    
    public Integer getAge() {
        if (birthYear != null) {
            try {
                return java.time.LocalDate.now().getYear() - birthYear;
            } catch (Exception e) {
                return java.time.LocalDate.now().getYear() - birthYear;
            }
        }
        return null;
    }
}
