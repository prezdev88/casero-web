package cl.casero.migration.service;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.service.dto.CreateCustomerForm;

import cl.casero.migration.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {
    Page<Customer> search(String filter, Pageable pageable);

    Customer get(Long id);

    Customer create(CreateCustomerForm form);

    void updateAddress(Long id, String address);

    void updateName(Long id, String name);

    Page<Customer> getTopDebtors(Pageable pageable);

    Page<Customer> getBestCustomers(Pageable pageable);

    long count();

    Page<CustomerRepository.SectorCountView> getCustomersCountBySector(Pageable pageable);
}
