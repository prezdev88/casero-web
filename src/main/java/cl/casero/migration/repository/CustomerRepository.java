package cl.casero.migration.repository;

import cl.casero.migration.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}
