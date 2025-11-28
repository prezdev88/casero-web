package cl.casero.migration.service.impl;

import cl.casero.migration.domain.MonthlyStatistic;
import cl.casero.migration.domain.enums.SaleType;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.repository.StatisticRepository;
import cl.casero.migration.repository.TransactionRepository;
import cl.casero.migration.service.StatisticsService;
import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@AllArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final CustomerRepository customerRepository;
    private final StatisticRepository statisticRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public MonthlyStatistic getMonthlyStatistic(LocalDate start, LocalDate end) {
        MonthlyStatistic monthlyStatistic = new MonthlyStatistic();

        long finishedCards = transactionRepository.countFinishedCards(start, end);
        long newCards = statisticRepository.countByTypeAndSaleType(TransactionType.SALE, SaleType.NEW_SALE, start, end);
        long maintenance = statisticRepository.countByTypeAndSaleType(TransactionType.SALE, SaleType.MAINTENANCE, start, end);

        Integer totalItems = statisticRepository.sumItemsByType(TransactionType.SALE, start, end);
        Integer payments = statisticRepository.sumAmountByType(TransactionType.PAYMENT, start, end);
        Integer sales = statisticRepository.sumAmountByType(TransactionType.SALE, start, end);

        monthlyStatistic.setFinishedCardsCount((int) finishedCards);
        monthlyStatistic.setNewCardsCount((int) newCards);
        monthlyStatistic.setMaintenanceCount((int) maintenance);
        monthlyStatistic.setTotalItemsCount(totalItems == null ? 0 : totalItems);
        monthlyStatistic.setPaymentsCount(payments == null ? 0 : payments);
        monthlyStatistic.setSalesCount(sales == null ? 0 : sales);

        return monthlyStatistic;
    }

    @Override
    public MonthlyStatistic getMonthlyStatistic(int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        
        return getMonthlyStatistic(start, end);
    }

    @Override
    public int getTotalDebt() {
        Integer total = customerRepository.getTotalDebt();
        return total == null ? 0 : total;
    }

    @Override
    public int getAverageDebt() {
        Double average = customerRepository.getAverageDebt();
        return average == null ? 0 : average.intValue();
    }

    @Override
    public long getCustomersCount() {
        return customerRepository.count();
    }
}
