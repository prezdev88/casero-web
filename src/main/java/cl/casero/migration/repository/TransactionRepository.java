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
            WITH payment_data AS (
                SELECT
                    t.customer_id,
                    t.date,
                    ROW_NUMBER() OVER (PARTITION BY t.customer_id ORDER BY t.date DESC) AS rn_desc,
                    LAG(t.date) OVER (PARTITION BY t.customer_id ORDER BY t.date) AS prev_date
                FROM transaction t
                WHERE t.customer_id IN (:customerIds)
                  AND t.type IN ('PAYMENT', 'REFUND', 'DEBT_FORGIVENESS', 'FAULT_DISCOUNT')
            ),
            payment_intervals AS (
                SELECT
                    customer_id,
                    date,
                    rn_desc,
                    prev_date,
                    CASE WHEN prev_date IS NULL THEN NULL ELSE (date - prev_date) END AS interval_days
                FROM payment_data
            ),
            payment_summary AS (
                SELECT
                    customer_id,
                    MAX(CASE WHEN rn_desc = 1 THEN date END) AS last_payment_date,
                    COUNT(*) AS payments_count,
                    MAX(interval_days) AS max_interval_days,
                    SUM(CASE WHEN interval_days IS NOT NULL THEN interval_days ELSE 0 END) AS total_interval_days,
                    SUM(CASE WHEN interval_days IS NOT NULL THEN 1 ELSE 0 END)::int AS interval_count,
                    SUM(CASE WHEN interval_days > 45 THEN 1 ELSE 0 END)::int AS late_interval_count
                FROM payment_intervals
                GROUP BY customer_id
            )
            SELECT
                c.id AS customerId,
                ps.last_payment_date AS lastPaymentDate,
                ps.max_interval_days AS maxIntervalBetweenPayments,
                ps.total_interval_days AS totalIntervalDays,
                ps.interval_count AS intervalCount,
                ps.late_interval_count AS lateIntervalCount,
                COALESCE(ps.payments_count, 0) AS totalPayments
            FROM customer c
            LEFT JOIN payment_summary ps ON ps.customer_id = c.id
            WHERE c.id IN (:customerIds)
            """, nativeQuery = true)
    List<CustomerScoreProjection> findCustomerScoreStats(@Param("customerIds") List<Long> customerIds);

    interface CustomerScoreProjection {
        Long getCustomerId();
        Integer getTotalPayments();
        LocalDate getLastPaymentDate();
        Integer getMaxIntervalBetweenPayments();
        Long getTotalIntervalDays();
        Integer getIntervalCount();
        Integer getLateIntervalCount();
    }
}
