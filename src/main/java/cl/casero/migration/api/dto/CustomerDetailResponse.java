package cl.casero.migration.api.dto;

import java.util.List;

public record CustomerDetailResponse(CustomerSummaryResponse customer,
                                     CustomerScoreResponse score,
                                     List<TransactionResponse> recentTransactions) {
}
