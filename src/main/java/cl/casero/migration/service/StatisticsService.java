package cl.casero.migration.service;

import cl.casero.migration.domain.MonthlyStatistic;

import java.time.LocalDate;

public interface StatisticsService {

    MonthlyStatistic getMonthlyStatistic(LocalDate start, LocalDate end);

    MonthlyStatistic getMonthlyStatistic(int month, int year);

    int getTotalDebt();

    int getAverageDebt();

    long getCustomersCount();
}
