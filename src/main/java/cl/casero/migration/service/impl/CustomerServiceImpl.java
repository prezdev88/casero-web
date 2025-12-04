package cl.casero.migration.service.impl;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.service.SectorService;
import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.dto.CreateCustomerForm;
import cl.casero.migration.service.dto.OverdueCustomerSummary;
import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final SectorService sectorService;
    private final CustomerRepository customerRepository;

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
        customer.setDebt(0);
        
        return customerRepository.save(customer);
    }

    @Override
    public void updateAddress(Long id, String address) {
        Customer customer = get(id);
        customer.setAddress(address);
        customerRepository.save(customer);
    }

    @Override
    public void updateName(Long id, String name) {
        Customer customer = get(id);
        customer.setName(name.trim());
        customerRepository.save(customer);
    }

    @Override
    public void updateSector(Long id, Long sectorId) {
        Customer customer = get(id);
        customer.setSector(sectorService.get(sectorId));
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
    public Page<OverdueCustomerSummary> getOverdueCustomers(Pageable pageable, int months) {
        int sanitizedMonths = Math.max(months, 1);
        return customerRepository.findOverdueCustomers(pageable, sanitizedMonths)
                .map(view -> new OverdueCustomerSummary(
                        view.getId(),
                        view.getName(),
                        view.getSector(),
                        view.getDebt(),
                        view.getLast_payment(),
                        view.getMonths_overdue()
                ));
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
