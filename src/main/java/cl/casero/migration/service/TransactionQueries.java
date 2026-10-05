package cl.casero.migration.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.repository.TransactionRepository.TopCustomerProjection;
import cl.casero.migration.service.dto.TransactionMonthlySummary;

public interface TransactionQueries {

    Page<Transaction> listAll(TransactionType type, Pageable pageable);

    Page<Transaction> listByCustomer(Long customerId, Pageable pageable);

    List<Transaction> listAllByCustomer(Long customerId);

    List<Transaction> listRecentByCustomer(Long customerId, int limit);

    List<TransactionMonthlySummary> getMonthlySummary(LocalDate start, LocalDate end);

    List<Transaction> getFinishedCardsThisMonth();

    List<Transaction> getSalesThisMonth();

    List<TopCustomerProjection> getTopCustomersThisMonth();

    long getSalesSum(LocalDate start, LocalDate end);

    long getPaymentsSum(LocalDate start, LocalDate end);
}
