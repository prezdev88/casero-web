package cl.casero.migration.repository;

import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByCustomerId(Long customerId, Pageable pageable);

    Transaction findTopByCustomerIdOrderByCreatedAtDescIdDesc(Long customerId);

    Page<Transaction> findByType(TransactionType type, Pageable pageable);

    @Query("""
            SELECT COUNT(t)
            FROM Transaction t
            WHERE t.balance = 0
              AND t.date BETWEEN :start AND :end
            """)
    long countFinishedCards(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            SELECT SUM(t.amount)
            FROM Transaction t
            WHERE t.type = :type
              AND t.date BETWEEN :start AND :end
            """)
    Integer sumByTypeAndDateRange(@Param("type") TransactionType type,
                                  @Param("start") LocalDate start,
                                  @Param("end") LocalDate end);

    List<Transaction> findByDateBetween(LocalDate start, LocalDate end);

    @Query(value = """
            SELECT
                t.customer_id AS customerId,
                COALESCE(SUM(CASE WHEN t.type IN ('SALE', 'INITIAL_BALANCE') THEN t.amount ELSE 0 END), 0) AS totalCharges,
                COALESCE(SUM(CASE WHEN t.type IN ('PAYMENT', 'REFUND', 'DEBT_FORGIVENESS', 'FAULT_DISCOUNT') THEN t.amount ELSE 0 END), 0) AS totalPayments,
                MAX(CASE WHEN t.type IN ('PAYMENT', 'REFUND', 'DEBT_FORGIVENESS', 'FAULT_DISCOUNT') THEN t.date ELSE NULL END) AS lastPaymentDate,
                MAX(t.date) AS lastActivityDate
            FROM transaction t
            WHERE t.customer_id IN (:customerIds)
            GROUP BY t.customer_id
            """, nativeQuery = true)
    List<CustomerScoreProjection> findCustomerScoreStats(@Param("customerIds") List<Long> customerIds);

    interface CustomerScoreProjection {
        Long getCustomerId();
        Integer getTotalCharges();
        Integer getTotalPayments();
        LocalDate getLastPaymentDate();
        LocalDate getLastActivityDate();
    }
}
