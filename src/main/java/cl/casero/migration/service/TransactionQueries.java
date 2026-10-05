package cl.casero.migration.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.dto.TopCustomerSummary;
import cl.casero.migration.service.dto.TransactionDetails;
import cl.casero.migration.service.dto.TransactionMonthlySummary;

public interface TransactionQueries {

    Page<TransactionDetails> listAll(TransactionType type, Pageable pageable);

    Page<TransactionDetails> listByCustomer(Long customerId, Pageable pageable);

    List<TransactionDetails> listAllByCustomer(Long customerId);

    List<TransactionDetails> listRecentByCustomer(Long customerId, int limit);

    List<TransactionMonthlySummary> getMonthlySummary(LocalDate start, LocalDate end);

    List<TransactionDetails> getFinishedCardsThisMonth();

    List<TransactionDetails> getSalesThisMonth();

    List<TopCustomerSummary> getTopCustomersThisMonth();

    long getSalesSum(LocalDate start, LocalDate end);

    long getPaymentsSum(LocalDate start, LocalDate end);
}
