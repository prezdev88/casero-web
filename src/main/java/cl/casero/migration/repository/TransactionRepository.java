package cl.casero.migration.repository;

import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

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
}
