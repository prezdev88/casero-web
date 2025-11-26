package cl.casero.migration.service;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.repository.TransactionRepository;
import cl.casero.migration.util.CustomerScoreCalculator;
import cl.casero.migration.util.CustomerScoreNarrator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomerScoreService {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;

    public CustomerScoreService(TransactionRepository transactionRepository,
                                CustomerRepository customerRepository) {
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
    }

    public Map<Long, CustomerScoreCalculator.ScoreResult> calculateScoreSummaries(Collection<Customer> customers) {
        if (customers == null || customers.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> ids = customers.stream()
                .map(Customer::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, TransactionRepository.CustomerScoreProjection> stats = fetchStats(ids);
        Map<Long, CustomerScoreCalculator.ScoreResult> summaries = new HashMap<>();
        for (Customer customer : customers) {
            Long id = customer.getId();
            if (id == null) {
                continue;
            }
            TransactionRepository.CustomerScoreProjection projection = stats.get(id);
            CustomerScoreCalculator.ScoreInputs inputs = buildInputs(customer, projection);
            CustomerScoreCalculator.ScoreResult summary = CustomerScoreCalculator.evaluate(inputs);
            summaries.put(id, summary);
        }
        return summaries;
    }

    public Map<Long, Double> calculateScores(Collection<Customer> customers) {
        return calculateScoreSummaries(customers)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().score()));
    }

    public double calculateScore(Customer customer) {
        if (customer == null) {
            return CustomerScoreCalculator.minScore();
        }
        Map<Long, CustomerScoreCalculator.ScoreResult> summaries = calculateScoreSummaries(List.of(customer));
        CustomerScoreCalculator.ScoreResult summary = summaries.get(customer.getId());
        return summary != null ? summary.score() : CustomerScoreCalculator.minScore();
    }

    public ScorePresentation getScorePresentation(Customer customer) {
        if (customer == null) {
            return new ScorePresentation(CustomerScoreCalculator.minScore(), "");
        }
        Map<Long, CustomerScoreCalculator.ScoreResult> summaries = calculateScoreSummaries(List.of(customer));
        CustomerScoreCalculator.ScoreResult summary = summaries.get(customer.getId());
        if (summary == null) {
            summary = CustomerScoreCalculator.evaluate(new CustomerScoreCalculator.ScoreInputs(
                    0, null, null, null, null, null, false));
        }
        String explanation = CustomerScoreNarrator.buildExplanation(summary);
        return new ScorePresentation(summary.score(), explanation);
    }

    public Page<RankingEntry> getRanking(Pageable pageable, boolean ascending) {
        Pageable effectivePageable = pageable == null ? PageRequest.of(0, 20) : pageable;
        List<Customer> customers = customerRepository.findAll();
        if (customers.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), effectivePageable, 0);
        }

        Map<Long, CustomerScoreCalculator.ScoreResult> summaries = calculateScoreSummaries(customers);
        List<RankingEntry> ranking = customers.stream()
                .map(customer -> {
                    CustomerScoreCalculator.ScoreResult summary = summaries.get(customer.getId());
                    if (summary == null) {
                        summary = CustomerScoreCalculator.evaluate(new CustomerScoreCalculator.ScoreInputs(
                                0, null, null, null, null, null, false));
                    }
                    String explanation = CustomerScoreNarrator.buildExplanation(summary);
                    return new RankingEntry(
                            customer.getId(),
                            customer.getName(),
                            summary.score(),
                            explanation);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        ranking.sort((a, b) -> {
            int scoreCompare = ascending
                    ? a.score.compareTo(b.score)
                    : b.score.compareTo(a.score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            String nameA = a.name == null ? "" : a.name;
            String nameB = b.name == null ? "" : b.name;
            return String.CASE_INSENSITIVE_ORDER.compare(nameA, nameB);
        });

        int total = ranking.size();
        int start = (int) effectivePageable.getOffset();
        int pageSize = effectivePageable.getPageSize();
        if (pageSize <= 0) {
            pageSize = 20;
        }
        if (start >= total) {
            int lastPage = Math.max((int) Math.ceil((double) total / pageSize) - 1, 0);
            int newStart = lastPage * pageSize;
            int newEnd = Math.min(newStart + pageSize, total);
            Pageable lastPageable = PageRequest.of(lastPage, pageSize, effectivePageable.getSort());
            return new PageImpl<>(ranking.subList(newStart, newEnd), lastPageable, total);
        }
        int end = Math.min(start + pageSize, total);
        List<RankingEntry> pageContent = ranking.subList(start, end);
        return new PageImpl<>(pageContent, effectivePageable, total);
    }

    public static final class RankingEntry {
        private final Long id;
        private final String name;
        private final Double score;
        private final String explanation;

        public RankingEntry(Long id, String name, Double score, String explanation) {
            this.id = id;
            this.name = name;
            this.score = score;
            this.explanation = explanation;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Double getScore() {
            return score;
        }

        public String getExplanation() {
            return explanation;
        }
    }

    public record ScorePresentation(double score, String explanation) {
    }

    private Map<Long, TransactionRepository.CustomerScoreProjection> fetchStats(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return transactionRepository.findCustomerScoreStats(List.copyOf(ids))
                .stream()
                .collect(Collectors.toMap(TransactionRepository.CustomerScoreProjection::getCustomerId, projection -> projection));
    }

    private CustomerScoreCalculator.ScoreInputs buildInputs(Customer customer,
                                                            TransactionRepository.CustomerScoreProjection projection) {
        boolean hasOutstandingDebt = customer != null && customer.getDebt() != null && customer.getDebt() > 0;
        return new CustomerScoreCalculator.ScoreInputs(
                projection != null ? projection.getTotalPayments() : 0,
                projection != null ? projection.getLastPaymentDate() : null,
                projection != null ? projection.getMaxIntervalBetweenPayments() : null,
                projection != null ? projection.getTotalIntervalDays() : null,
                projection != null ? projection.getIntervalCount() : null,
                projection != null ? projection.getLateIntervalCount() : null,
                hasOutstandingDebt
        );
    }
}
