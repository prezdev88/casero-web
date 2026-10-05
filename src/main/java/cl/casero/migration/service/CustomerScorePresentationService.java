package cl.casero.migration.service;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.service.dto.CustomerScorePresentation;
import cl.casero.migration.util.CustomerScoreCalculator;
import cl.casero.migration.util.CustomerScoreNarrator;
import cl.casero.migration.util.CustomerScoreSummary;
import cl.casero.migration.util.CustomerScoreSummary.CycleScore;

@Service
@RequiredArgsConstructor
public class CustomerScorePresentationService {

    private final CustomerScoreService customerScoreService;

    public CustomerScorePresentation getScorePresentation(Customer customer) {
        if (customer == null) {
            double minimumScore = CustomerScoreCalculator.minScore();
            List<CycleScore> emptyCycles = List.of();
            return new CustomerScorePresentation(minimumScore, "", emptyCycles);
        }

        List<Customer> customers = List.of(customer);
        Map<Long, CustomerScoreSummary> summaries = customerScoreService.calculateScoreSummaries(customers);
        Long customerId = customer.getId();
        CustomerScoreSummary summary = summaries.get(customerId);
        return present(summary);
    }

    public CustomerScorePresentation present(CustomerScoreSummary summary) {
        CustomerScoreSummary effectiveSummary = summary;
        if (effectiveSummary == null) {
            double minimumScore = CustomerScoreCalculator.minScore();
            List<CycleScore> emptyCycles = List.of();
            effectiveSummary = new CustomerScoreSummary(minimumScore, emptyCycles);
        }

        double score = effectiveSummary.score();
        String explanation = CustomerScoreNarrator.buildExplanation(effectiveSummary);
        List<CycleScore> cycles = effectiveSummary.cycles();
        return new CustomerScorePresentation(score, explanation, cycles);
    }
}
