package cl.casero.migration.service.impl;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.repository.CustomerRepository.OverdueCustomerView;
import cl.casero.migration.repository.CustomerRepository.SectorCountView;
import cl.casero.migration.service.CustomerNotFoundException;
import cl.casero.migration.service.CustomerQueries;
import cl.casero.migration.service.dto.CustomerBirthdayDTO;
import cl.casero.migration.service.dto.CustomerDetails;
import cl.casero.migration.service.dto.OverdueCustomerSummary;
import cl.casero.migration.service.dto.SectorCustomerCount;
import cl.casero.migration.service.mapping.ReadModelMapper;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerQueryService implements CustomerQueries {

    private final CustomerRepository customerRepository;

    @Override
    public Page<CustomerDetails> search(String filter, Pageable pageable) {
        if (filter == null || filter.isBlank()) {
            return Page.empty(pageable);
        }

        String trimmedFilter = filter.trim();
        Page<Customer> customers = customerRepository.search(trimmedFilter, pageable);
        return customers.map(ReadModelMapper::customer);
    }

    @Override
    public CustomerDetails get(Long id) {
        Optional<Customer> result = customerRepository.findByIdAndEnabledTrue(id);
        Customer customer = result.orElseThrow(() -> new CustomerNotFoundException(id));
        return ReadModelMapper.customer(customer);
    }

    @Override
    public Page<CustomerDetails> getTopDebtors(Pageable pageable) {
        Page<Customer> customers = customerRepository.findAllByEnabledTrueOrderByDebtDesc(pageable);
        return customers.map(ReadModelMapper::customer);
    }

    @Override
    public Page<CustomerDetails> getBestCustomers(Pageable pageable) {
        Page<Customer> customers = customerRepository.findAllByEnabledTrueOrderByDebtAsc(pageable);
        return customers.map(ReadModelMapper::customer);
    }

    @Override
    public Page<OverdueCustomerSummary> getOverdueCustomers(Pageable pageable, int months) {
        int sanitizedMonths = Math.max(months, 1);
        Page<OverdueCustomerView> overdueCustomers = customerRepository.findOverdueCustomers(pageable, sanitizedMonths);
        return overdueCustomers.map(this::toOverdueCustomerSummary);
    }

    @Override
    public long getOverdueDebt(int months) {
        int sanitizedMonths = Math.max(months, 1);
        return customerRepository.sumOverdueDebt(sanitizedMonths);
    }

    @Override
    public long count() {
        return customerRepository.countByEnabledTrue();
    }

    @Override
    public Page<SectorCustomerCount> getCustomersCountBySector(Pageable pageable) {
        Page<SectorCountView> sectorCounts = customerRepository.countBySector(pageable);
        return sectorCounts.map(this::toSectorCustomerCount);
    }

    @Override
    public long getBirthdaysThisMonthCount(int month) {
        return customerRepository.countBirthdaysThisMonth(month);
    }

    @Override
    public List<CustomerBirthdayDTO> getBirthdaysThisMonth(int month) {
        return customerRepository.findBirthdaysThisMonth(month);
    }

    private OverdueCustomerSummary toOverdueCustomerSummary(OverdueCustomerView view) {
        Long id = view.getId();
        String name = view.getName();
        String sector = view.getSector();
        Integer debt = view.getDebt();
        String lastPayment = view.getLast_payment();
        Integer monthsOverdue = view.getMonths_overdue();
        return new OverdueCustomerSummary(id, name, sector, debt, lastPayment, monthsOverdue);
    }

    private SectorCustomerCount toSectorCustomerCount(SectorCountView view) {
        String name = view.getName();
        long total = view.getTotal();
        return new SectorCustomerCount(name, total);
    }
}
