package cl.casero.migration.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonthlyStatistic {
    private int finishedCardsCount;
    private int newCardsCount;
    private int maintenanceCount;
    private int totalItemsCount;
    private int paymentsCount;
    private int salesCount;
}
