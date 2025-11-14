package cl.casero.migration.repository;

import cl.casero.migration.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("""
            SELECT c FROM Customer c
            JOIN c.sector s
            WHERE translate(lower(c.name), 'áéíóúñ', 'aeioun') LIKE translate(lower(concat('%', :filter, '%')), 'áéíóúñ', 'aeioun')
               OR translate(lower(c.address), 'áéíóúñ', 'aeioun') LIKE translate(lower(concat('%', :filter, '%')), 'áéíóúñ', 'aeioun')
               OR translate(lower(s.name), 'áéíóúñ', 'aeioun') LIKE translate(lower(concat('%', :filter, '%')), 'áéíóúñ', 'aeioun')
            ORDER BY c.name ASC
            """)
    List<Customer> search(@Param("filter") String filter);

    List<Customer> findTop10ByOrderByDebtDesc();

    List<Customer> findTop10ByOrderByDebtAsc();

    @Query("SELECT COALESCE(SUM(c.debt),0) FROM Customer c")
    Integer getTotalDebt();

    @Query("SELECT COALESCE(AVG(c.debt),0) FROM Customer c")
    Double getAverageDebt();

    @Query("""
            SELECT c.sector.name AS name, COUNT(c) AS total
            FROM Customer c
            GROUP BY c.sector.name
            ORDER BY c.sector.name
            """)
    List<SectorCountView> countBySector();

    interface SectorCountView {
        String getName();
        long getTotal();
    }
}
