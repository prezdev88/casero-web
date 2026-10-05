package cl.casero.migration.service.dto;

import lombok.Value;

@Value
public class TransactionCustomerSummary {
    Long id;
    String name;
    SectorSummary sector;
}
