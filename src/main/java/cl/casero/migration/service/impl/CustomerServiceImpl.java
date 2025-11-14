package cl.casero.migration.service.impl;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.service.SectorService;
import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.dto.CreateCustomerForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Page<Customer> search(String filter, Pageable pageable) {
        if (filter == null || filter.isBlank()) {
            return Page.empty(pageable);
        }
        return customerRepository.search(filter.trim(), pageable);
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
    public Page<Customer> getTopDebtors(Pageable pageable) {
        return customerRepository.findAllByOrderByDebtDesc(pageable);
    }

    @Override
    public Page<Customer> getBestCustomers(Pageable pageable) {
        return customerRepository.findAllByOrderByDebtAsc(pageable);
    }

    @Override
    public long count() {
        return customerRepository.count();
    }

    @Override
    public Page<CustomerRepository.SectorCountView> getCustomersCountBySector(Pageable pageable) {
        return customerRepository.countBySector(pageable);
    }
}
