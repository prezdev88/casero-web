package cl.casero.migration.service.impl;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Sector;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.service.CustomerCommands;
import cl.casero.migration.service.CustomerNotFoundException;
import cl.casero.migration.service.SectorService;
import cl.casero.migration.service.dto.CreateCustomerForm;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerCommandService implements CustomerCommands {

    private final SectorService sectorService;
    private final CustomerRepository customerRepository;

    @Override
    public Customer create(CreateCustomerForm form) {
        Customer customer = new Customer();
        String name = form.getName();
        String trimmedName = name.trim();
        customer.setName(trimmedName);
        Long sectorId = form.getSectorId();
        Sector sector = sectorService.get(sectorId);
        customer.setSector(sector);
        String address = form.getAddress();
        String trimmedAddress = address.trim();
        customer.setAddress(trimmedAddress);
        customer.setDebt(0);
        customer.setEnabled(true);

        return customerRepository.save(customer);
    }

    @Override
    public void delete(Long id) {
        Customer customer = getCustomer(id);
        customer.setEnabled(false);
        customerRepository.save(customer);
    }

    @Override
    public void updateAddress(Long id, String address) {
        Customer customer = getCustomer(id);
        customer.setAddress(address);
        customerRepository.save(customer);
    }

    @Override
    public void updateName(Long id, String name) {
        Customer customer = getCustomer(id);
        String trimmedName = name.trim();
        customer.setName(trimmedName);
        customerRepository.save(customer);
    }

    @Override
    public void updateSector(Long id, Long sectorId) {
        Customer customer = getCustomer(id);
        Sector sector = sectorService.get(sectorId);
        customer.setSector(sector);
        customerRepository.save(customer);
    }

    @Override
    public void updateBirthdate(Long id, Integer day, Integer month, Integer year) {
        Customer customer = getCustomer(id);
        customer.setBirthDay(day);
        customer.setBirthMonth(month);
        customer.setBirthYear(year);
        customerRepository.save(customer);
    }

    private Customer getCustomer(Long id) {
        Optional<Customer> result = customerRepository.findByIdAndEnabledTrue(id);
        return result.orElseThrow(() -> new CustomerNotFoundException(id));
    }
}
