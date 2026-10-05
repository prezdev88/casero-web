package cl.casero.migration.service.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.Value;

import cl.casero.migration.domain.enums.TransactionType;

@Value
public class TransactionDetails {
    Long id;
    LocalDate date;
    String detail;
    Integer amount;
    Integer balance;
    TransactionType type;
    OffsetDateTime createdAt;
    Integer itemCount;
    TransactionCustomerSummary customer;
}
