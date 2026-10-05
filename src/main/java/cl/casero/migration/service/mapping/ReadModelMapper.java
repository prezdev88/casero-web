package cl.casero.migration.service.mapping;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Sector;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.dto.CustomerDetails;
import cl.casero.migration.service.dto.CustomerScoreInput;
import cl.casero.migration.service.dto.SectorSummary;
import cl.casero.migration.service.dto.TransactionCustomerSummary;
import cl.casero.migration.service.dto.TransactionDetails;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReadModelMapper {

    public static CustomerDetails customer(Customer entity) {
        Long id = entity.getId();
        String name = entity.getName();
        String address = entity.getAddress();
        Integer debt = entity.getDebt();
        Sector sector = entity.getSector();
        SectorSummary sectorData = sector(sector);
        Integer day = entity.getBirthDay();
        Integer month = entity.getBirthMonth();
        Integer year = entity.getBirthYear();
        return new CustomerDetails(id, name, address, debt, sectorData, day, month, year);
    }

    public static TransactionDetails transaction(Transaction entity) {
        Long id = entity.getId();
        LocalDate date = entity.getDate();
        String detail = entity.getDetail();
        Integer amount = entity.getAmount();
        Integer balance = entity.getBalance();
        TransactionType type = entity.getType();
        OffsetDateTime createdAt = entity.getCreatedAt();
        Integer itemCount = entity.getItemCount();
        Customer customer = entity.getCustomer();
        TransactionCustomerSummary customerData = transactionCustomer(customer);
        return new TransactionDetails(id, date, detail, amount, balance, type, createdAt, itemCount, customerData);
    }

    public static CustomerScoreInput scoreInput(CustomerDetails customer) {
        Long id = customer.getId();
        Integer debt = customer.getDebt();
        return new CustomerScoreInput(id, debt);
    }

    private static TransactionCustomerSummary transactionCustomer(Customer customer) {
        if (customer == null) {
            return null;
        }

        Long id = customer.getId();
        String name = customer.getName();
        Sector sector = customer.getSector();
        SectorSummary sectorData = sector(sector);
        return new TransactionCustomerSummary(id, name, sectorData);
    }

    private static SectorSummary sector(Sector entity) {
        if (entity == null) {
            return null;
        }

        Long id = entity.getId();
        String name = entity.getName();
        return new SectorSummary(id, name);
    }
}
