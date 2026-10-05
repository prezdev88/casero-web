package cl.casero.migration.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.service.dto.CustomerBirthdayDTO;
import cl.casero.migration.service.dto.OverdueCustomerSummary;
import cl.casero.migration.service.dto.SectorCustomerCount;

public interface CustomerQueries {

    Page<Customer> search(String filter, Pageable pageable);

    Customer get(Long id);

    Page<Customer> getTopDebtors(Pageable pageable);

    Page<Customer> getBestCustomers(Pageable pageable);

    Page<OverdueCustomerSummary> getOverdueCustomers(Pageable pageable, int months);

    long getOverdueDebt(int months);

    long count();

    Page<SectorCustomerCount> getCustomersCountBySector(Pageable pageable);

    long getBirthdaysThisMonthCount(int month);

    List<CustomerBirthdayDTO> getBirthdaysThisMonth(int month);
}
