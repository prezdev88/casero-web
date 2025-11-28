package cl.casero.migration.api.dto;

import cl.casero.migration.domain.enums.TransactionType;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TransactionResponse(Long id,
                                  Long customerId,
                                  LocalDate date,
                                  String detail,
                                  Integer amount,
                                  Integer balance,
                                  TransactionType type,
                                  OffsetDateTime createdAt,
                                  Integer itemCount) {
}
