package cl.casero.migration.repository;

import cl.casero.migration.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query(value = """
            SELECT c FROM Customer c
            JOIN c.sector s
            WHERE translate(lower(c.name), 'áéíóúñ', 'aeioun') LIKE translate(lower(concat('%', :filter, '%')), 'áéíóúñ', 'aeioun')
               OR translate(lower(c.address), 'áéíóúñ', 'aeioun') LIKE translate(lower(concat('%', :filter, '%')), 'áéíóúñ', 'aeioun')
               OR translate(lower(s.name), 'áéíóúñ', 'aeioun') LIKE translate(lower(concat('%', :filter, '%')), 'áéíóúñ', 'aeioun')
            ORDER BY c.name ASC
            """,
            countQuery = """
            SELECT COUNT(c) FROM Customer c
            JOIN c.sector s
            WHERE translate(lower(c.name), 'áéíóúñ', 'aeioun') LIKE translate(lower(concat('%', :filter, '%')), 'áéíóúñ', 'aeioun')
               OR translate(lower(c.address), 'áéíóúñ', 'aeioun') LIKE translate(lower(concat('%', :filter, '%')), 'áéíóúñ', 'aeioun')
               OR translate(lower(s.name), 'áéíóúñ', 'aeioun') LIKE translate(lower(concat('%', :filter, '%')), 'áéíóúñ', 'aeioun')
            """)
    Page<Customer> search(@Param("filter") String filter, Pageable pageable);

    Page<Customer> findAllByOrderByDebtDesc(Pageable pageable);

    Page<Customer> findAllByOrderByDebtAsc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.debt),0) FROM Customer c")
    Integer getTotalDebt();

    @Query("SELECT COALESCE(AVG(c.debt),0) FROM Customer c")
    Double getAverageDebt();

    @Query(value = """
            SELECT c.sector.name AS name, COUNT(c) AS total
            FROM Customer c
            GROUP BY c.sector.name
            ORDER BY c.sector.name
            """,
            countQuery = """
            SELECT COUNT(DISTINCT c.sector.name)
            FROM Customer c
            """)
    Page<SectorCountView> countBySector(Pageable pageable);

    interface SectorCountView {
        String getName();
        long getTotal();
    }

    @Query(value = """
            WITH last_payments AS (
                SELECT
                    c.id,
                    c.name,
                    s.name AS sector,
                    c.debt,
                    MAX(t.date) AS last_payment_date
                FROM customer c
                JOIN sector s ON s.id = c.sector_id
                LEFT JOIN transaction t
                    ON t.customer_id = c.id
                   AND t.type = 'PAYMENT'
                WHERE c.debt > 0
                GROUP BY c.id, c.name, s.name, c.debt
            )
            SELECT
                lp.id,
                lp.name,
                lp.sector,
                lp.debt,
                CASE
                    WHEN lp.last_payment_date IS NULL THEN 'Nunca ha abonado'
                    ELSE TO_CHAR(lp.last_payment_date, 'YYYY-MM-DD')
                END AS last_payment,
                CASE
                    WHEN lp.last_payment_date IS NULL THEN NULL
                    ELSE CAST(
                        DATE_PART('year', age(CURRENT_DATE, lp.last_payment_date)) * 12
                        + DATE_PART('month', age(CURRENT_DATE, lp.last_payment_date))
                    AS INTEGER)
                END AS months_overdue
            FROM last_payments lp
            WHERE lp.last_payment_date IS NULL
               OR lp.last_payment_date < CURRENT_DATE - (:months * INTERVAL '1 month')
            ORDER BY
                (lp.last_payment_date IS NULL),
                lp.last_payment_date DESC
            """,
            countQuery = """
            WITH last_payments AS (
                SELECT
                    c.id,
                    c.name,
                    s.name AS sector,
                    c.debt,
                    MAX(t.date) AS last_payment_date
                FROM customer c
                JOIN sector s ON s.id = c.sector_id
                LEFT JOIN transaction t
                    ON t.customer_id = c.id
                   AND t.type = 'PAYMENT'
                WHERE c.debt > 0
                GROUP BY c.id, c.name, s.name, c.debt
            )
            SELECT COUNT(*)
            FROM last_payments lp
            WHERE lp.last_payment_date IS NULL
               OR lp.last_payment_date < CURRENT_DATE - (:months * INTERVAL '1 month')
            """,
            nativeQuery = true)
    Page<OverdueCustomerView> findOverdueCustomers(Pageable pageable, @Param("months") int months);

    interface OverdueCustomerView {
        Long getId();
        String getName();
        String getSector();
        Integer getDebt();
        String getLast_payment();
        Integer getMonths_overdue();
    }
}
