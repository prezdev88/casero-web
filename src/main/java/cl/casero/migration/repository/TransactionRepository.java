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

    List<Transaction> findByCustomerIdOrderByDateDescIdDesc(Long customerId);

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
            WITH ordered_transactions AS (
                SELECT
                    t.id,
                    t.customer_id,
                    t.date,
                    t.balance,
                    t.type,
                    ROW_NUMBER() OVER (PARTITION BY t.customer_id ORDER BY t.date, t.id) AS seq,
                    COALESCE(
                        SUM(CASE WHEN t.balance = 0 THEN 1 ELSE 0 END)
                            OVER (PARTITION BY t.customer_id ORDER BY t.date, t.id ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING),
                        0) AS cycle_group
                FROM transaction t
                WHERE t.customer_id IN (:customerIds)
            ),
            cycle_bounds AS (
                SELECT
                    customer_id,
                    cycle_group,
                    MIN(date) AS cycle_start_date,
                    MAX(date) AS cycle_end_date,
                    MAX(seq) AS max_seq
                FROM ordered_transactions
                GROUP BY customer_id, cycle_group
            ),
            payments AS (
                SELECT
                    ot.customer_id,
                    ot.cycle_group,
                    ot.date,
                    ROW_NUMBER() OVER (PARTITION BY ot.customer_id, ot.cycle_group ORDER BY ot.date DESC, ot.id DESC) AS rn_desc,
                    LAG(ot.date) OVER (PARTITION BY ot.customer_id, ot.cycle_group ORDER BY ot.date, ot.id) AS prev_date
                FROM ordered_transactions ot
                WHERE ot.type IN ('PAYMENT', 'REFUND', 'DEBT_FORGIVENESS', 'FAULT_DISCOUNT')
            ),
            payment_intervals AS (
                SELECT
                    customer_id,
                    cycle_group,
                    date,
                    rn_desc,
                    CASE WHEN prev_date IS NULL THEN NULL ELSE (date - prev_date) END AS interval_days
                FROM payments
            ),
            payment_summary AS (
                SELECT
                    customer_id,
                    cycle_group,
                    MAX(CASE WHEN rn_desc = 1 THEN date END) AS last_payment_date,
                    COUNT(*) AS payments_count,
                    MAX(interval_days) AS max_interval_days,
                    SUM(CASE WHEN interval_days IS NOT NULL THEN interval_days ELSE 0 END) AS total_interval_days,
                    SUM(CASE WHEN interval_days IS NOT NULL THEN 1 ELSE 0 END)::int AS interval_count,
                    SUM(CASE WHEN interval_days > 45 THEN 1 ELSE 0 END)::int AS late_interval_count,
                    COUNT(DISTINCT TO_CHAR(date, 'YYYYMM')) AS payment_month_count
                FROM payment_intervals
                GROUP BY customer_id, cycle_group
            )
            SELECT
                cb.customer_id AS customerId,
                cb.cycle_group AS cycleGroup,
                cb.cycle_start_date AS cycleStartDate,
                cb.cycle_end_date AS cycleEndDate,
                ps.last_payment_date AS lastPaymentDate,
                COALESCE(ps.payments_count, 0) AS totalPayments,
                ps.max_interval_days AS maxIntervalBetweenPayments,
                ps.total_interval_days AS totalIntervalDays,
                ps.interval_count AS intervalCount,
                ps.late_interval_count AS lateIntervalCount,
                ps.payment_month_count AS paymentMonthCount,
                CASE
                    WHEN cb.cycle_start_date IS NULL OR cb.cycle_end_date IS NULL THEN NULL
                    ELSE (
                        (EXTRACT(YEAR FROM cb.cycle_end_date) - EXTRACT(YEAR FROM cb.cycle_start_date)) * 12
                        + (EXTRACT(MONTH FROM cb.cycle_end_date) - EXTRACT(MONTH FROM cb.cycle_start_date)) + 1
                    )::int
                END AS cycleMonthCount,
                CASE
                    WHEN MAX(CASE WHEN ot.seq = cb.max_seq THEN ot.balance ELSE NULL END) FILTER (WHERE cb.max_seq IS NOT NULL) > 0 THEN true
                    ELSE false
                END AS hasOutstandingDebt
            FROM cycle_bounds cb
            LEFT JOIN payment_summary ps ON ps.customer_id = cb.customer_id AND ps.cycle_group = cb.cycle_group
            LEFT JOIN ordered_transactions ot ON ot.customer_id = cb.customer_id AND ot.cycle_group = cb.cycle_group
            GROUP BY cb.customer_id, cb.cycle_group, cb.cycle_start_date, cb.cycle_end_date,
                     ps.last_payment_date, ps.payments_count, ps.max_interval_days, ps.total_interval_days,
                     ps.interval_count, ps.late_interval_count, ps.payment_month_count
            ORDER BY cb.customer_id, cb.cycle_group
            """, nativeQuery = true)
    List<CustomerCycleProjection> findCustomerCycleStats(@Param("customerIds") List<Long> customerIds);

    interface CustomerCycleProjection {
        Long getCustomerId();
        Integer getCycleGroup();
        LocalDate getCycleStartDate();
        LocalDate getCycleEndDate();
        LocalDate getLastPaymentDate();
        Integer getTotalPayments();
        Integer getMaxIntervalBetweenPayments();
        Long getTotalIntervalDays();
        Integer getIntervalCount();
        Integer getLateIntervalCount();
        Long getPaymentMonthCount();
        Integer getCycleMonthCount();
        Boolean getHasOutstandingDebt();
    }
}
