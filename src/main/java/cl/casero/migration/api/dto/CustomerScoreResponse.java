package cl.casero.migration.api.dto;

import java.util.List;

public record CustomerScoreResponse(double score,
                                    String explanation,
                                    List<CustomerScoreCycleResponse> cycles) {
}
