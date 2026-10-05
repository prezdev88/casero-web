package cl.casero.migration.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.casero.migration.service.dto.CustomerBirthdayDTO;
import cl.casero.migration.service.dto.CustomerDetails;
import cl.casero.migration.service.dto.OverdueCustomerSummary;
import cl.casero.migration.service.dto.SectorCustomerCount;

public interface CustomerQueries {

    Page<CustomerDetails> search(String filter, Pageable pageable);

    CustomerDetails get(Long id);

    Page<CustomerDetails> getTopDebtors(Pageable pageable);

    Page<CustomerDetails> getBestCustomers(Pageable pageable);

    Page<OverdueCustomerSummary> getOverdueCustomers(Pageable pageable, int months);

    long getOverdueDebt(int months);

    long count();

    Page<SectorCustomerCount> getCustomersCountBySector(Pageable pageable);

    long getBirthdaysThisMonthCount(int month);

    List<CustomerBirthdayDTO> getBirthdaysThisMonth(int month);
}
