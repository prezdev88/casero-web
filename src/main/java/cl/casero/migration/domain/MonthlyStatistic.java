package cl.casero.migration.domain;

public class MonthlyStatistic {

    private int finishedCardsCount;
    private int newCardsCount;
    private int maintenanceCount;
    private int totalItemsCount;
    private int paymentsCount;
    private int salesCount;

    public int getFinishedCardsCount() {
        return finishedCardsCount;
    }

    public void setFinishedCardsCount(int finishedCardsCount) {
        this.finishedCardsCount = finishedCardsCount;
    }

    public int getNewCardsCount() {
        return newCardsCount;
    }

    public void setNewCardsCount(int newCardsCount) {
        this.newCardsCount = newCardsCount;
    }

    public int getMaintenanceCount() {
        return maintenanceCount;
    }

    public void setMaintenanceCount(int maintenanceCount) {
        this.maintenanceCount = maintenanceCount;
    }

    public int getTotalItemsCount() {
        return totalItemsCount;
    }

    public void setTotalItemsCount(int totalItemsCount) {
        this.totalItemsCount = totalItemsCount;
    }

    public int getPaymentsCount() {
        return paymentsCount;
    }

    public void setPaymentsCount(int paymentsCount) {
        this.paymentsCount = paymentsCount;
    }

    public int getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(int salesCount) {
        this.salesCount = salesCount;
    }
}
