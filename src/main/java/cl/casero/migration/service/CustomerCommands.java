package cl.casero.migration.service;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.service.dto.CreateCustomerForm;

public interface CustomerCommands {

    Customer create(CreateCustomerForm form);

    void delete(Long id);

    void updateAddress(Long id, String address);

    void updateName(Long id, String name);

    void updateSector(Long id, Long sectorId);

    void updateBirthdate(Long id, Integer day, Integer month, Integer year);
}
