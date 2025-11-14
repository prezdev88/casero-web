package cl.casero.migration.repository;

import cl.casero.migration.domain.Statistic;
import cl.casero.migration.domain.enums.SaleType;
import cl.casero.migration.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface StatisticRepository extends JpaRepository<Statistic, Long> {

    @Query("""
            SELECT COUNT(s)
            FROM Statistic s
            WHERE s.type = :type
              AND s.saleType = :saleType
              AND s.date BETWEEN :start AND :end
            """)
    long countByTypeAndSaleType(@Param("type") TransactionType type,
                                @Param("saleType") SaleType saleType,
                                @Param("start") LocalDate start,
                                @Param("end") LocalDate end);

    @Query("""
            SELECT SUM(s.itemsCount)
            FROM Statistic s
            WHERE s.type = :type
              AND s.date BETWEEN :start AND :end
            """)
    Integer sumItemsByType(@Param("type") TransactionType type,
                           @Param("start") LocalDate start,
                           @Param("end") LocalDate end);

    @Query("""
            SELECT SUM(s.amount)
            FROM Statistic s
            WHERE s.type = :type
              AND s.date BETWEEN :start AND :end
            """)
    Integer sumAmountByType(@Param("type") TransactionType type,
                            @Param("start") LocalDate start,
                            @Param("end") LocalDate end);

    @Transactional
    @Modifying
    @Query("""
            DELETE FROM Statistic s
            WHERE s.type = :type
              AND s.amount = :amount
              AND s.date = :date
            """)
    void deleteByTypeAndAmountAndDate(@Param("type") TransactionType type,
                                      @Param("amount") Integer amount,
                                      @Param("date") LocalDate date);
}
