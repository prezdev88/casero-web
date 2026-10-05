package cl.casero.migration.service.dto;

import lombok.Value;

import cl.casero.migration.domain.CustomerBirthDate;

@Value
public class CustomerDetails {
    Long id;
    String name;
    String address;
    Integer debt;
    SectorSummary sector;
    Integer birthDay;
    Integer birthMonth;
    Integer birthYear;

    public CustomerBirthDate getBirthDate() {
        return new CustomerBirthDate(birthDay, birthMonth, birthYear);
    }
}
