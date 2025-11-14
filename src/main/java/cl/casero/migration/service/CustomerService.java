package cl.casero.migration.service;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.service.dto.CreateCustomerForm;

import cl.casero.migration.repository.CustomerRepository;

import java.util.List;

public interface CustomerService {
    List<Customer> search(String filter);

    Customer get(Long id);

    Customer create(CreateCustomerForm form);

    void updateAddress(Long id, String address);

    List<Customer> getTopDebtors(int limit);

    List<Customer> getBestCustomers(int limit);

    long count();

    List<CustomerRepository.SectorCountView> getCustomersCountBySector();
}
