package cl.casero.migration.service.impl;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.service.SectorService;
import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.dto.CreateCustomerForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final SectorService sectorService;

    public CustomerServiceImpl(CustomerRepository customerRepository,
                               SectorService sectorService) {
        this.customerRepository = customerRepository;
        this.sectorService = sectorService;
    }

    @Override
    public List<Customer> search(String filter) {
        if (filter == null || filter.isBlank()) {
            return List.of();
        }
        return customerRepository.search(filter.trim());
    }

    @Override
    public Customer get(Long id) {
        return customerRepository.findById(id).orElseThrow();
    }

    @Override
    public Customer create(CreateCustomerForm form) {
        Customer customer = new Customer();
        customer.setName(form.getName().trim());
        customer.setSector(sectorService.get(form.getSectorId()));
        customer.setAddress(form.getAddress().trim());
        customer.setDebt(form.getInitialDebt());
        return customerRepository.save(customer);
    }

    @Override
    public void updateAddress(Long id, String address) {
        Customer customer = get(id);
        customer.setAddress(address);
        customerRepository.save(customer);
    }

    @Override
    public List<Customer> getTopDebtors(int limit) {
        return customerRepository.findTop10ByOrderByDebtDesc()
                .stream()
                .limit(limit)
                .toList();
    }

    @Override
    public List<Customer> getBestCustomers(int limit) {
        return customerRepository.findTop10ByOrderByDebtAsc()
                .stream()
                .limit(limit)
                .toList();
    }

    @Override
    public long count() {
        return customerRepository.count();
    }

    @Override
    public List<CustomerRepository.SectorCountView> getCustomersCountBySector() {
        return customerRepository.countBySector();
    }
}
