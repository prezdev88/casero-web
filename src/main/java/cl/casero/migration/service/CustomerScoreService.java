package cl.casero.migration.service;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.repository.TransactionRepository;
import cl.casero.migration.util.CustomerScoreCalculator;
import cl.casero.migration.util.CustomerScoreNarrator;
import cl.casero.migration.util.CustomerScoreSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
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

    public Map<Long, CustomerScoreSummary> calculateScoreSummaries(Collection<Customer> customers) {
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
        Map<Long, List<TransactionRepository.CustomerCycleProjection>> cycleStats = fetchCycleStats(ids);
        Map<Long, CustomerScoreSummary> summaries = new HashMap<>();
        for (Customer customer : customers) {
            if (customer == null) {
                continue;
            }
            Long id = customer.getId();
            if (id == null) {
                continue;
            }
            List<TransactionRepository.CustomerCycleProjection> cycles = cycleStats.getOrDefault(id, Collections.emptyList());
            CustomerScoreSummary summary = buildSummary(customer, cycles);
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
        Map<Long, CustomerScoreSummary> summaries = calculateScoreSummaries(List.of(customer));
        CustomerScoreSummary summary = summaries.get(customer.getId());
        return summary != null ? summary.score() : CustomerScoreCalculator.minScore();
    }

    public ScorePresentation getScorePresentation(Customer customer) {
        if (customer == null) {
            return new ScorePresentation(CustomerScoreCalculator.minScore(), "", Collections.emptyList());
        }
        Map<Long, CustomerScoreSummary> summaries = calculateScoreSummaries(List.of(customer));
        CustomerScoreSummary summary = summaries.get(customer.getId());
        if (summary == null) {
            summary = new CustomerScoreSummary(CustomerScoreCalculator.minScore(), Collections.emptyList());
        }
        String explanation = CustomerScoreNarrator.buildExplanation(summary);
        return new ScorePresentation(summary.score(), explanation, summary.cycles());
    }

    public Page<RankingEntry> getRanking(Pageable pageable, boolean ascending) {
        Pageable effectivePageable = pageable == null ? PageRequest.of(0, 20) : pageable;
        List<Customer> customers = customerRepository.findAll();
        if (customers.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), effectivePageable, 0);
        }

        Map<Long, CustomerScoreSummary> summaries = calculateScoreSummaries(customers);
        List<RankingEntry> ranking = customers.stream()
                .map(customer -> {
                    CustomerScoreSummary summary = summaries.get(customer.getId());
                    if (summary == null) {
                        summary = new CustomerScoreSummary(CustomerScoreCalculator.minScore(), Collections.emptyList());
                    }
                    String explanation = CustomerScoreNarrator.buildExplanation(summary);
                    return new RankingEntry(
                            customer.getId(),
                            customer.getName(),
                            customer.getDebt(),
                            summary.score(),
                            explanation,
                            summary.cycles().size());
                })
                .collect(Collectors.toCollection(ArrayList::new));

        ranking.sort((a, b) -> {
            int scoreCompare = ascending
                    ? a.score.compareTo(b.score)
                    : b.score.compareTo(a.score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            int cycleCompare = Integer.compare(b.cycleCount, a.cycleCount);
            if (cycleCompare != 0) {
                return cycleCompare;
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
        private final Integer debt;
        private final Double score;
        private final String explanation;
        private final int cycleCount;

        public RankingEntry(Long id, String name, Integer debt, Double score, String explanation, int cycleCount) {
            this.id = id;
            this.name = name;
            this.debt = debt;
            this.score = score;
            this.explanation = explanation;
            this.cycleCount = cycleCount;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getDebt() {
            return debt;
        }

        public Double getScore() {
            return score;
        }

        public String getExplanation() {
            return explanation;
        }

        public int getCycleCount() {
            return cycleCount;
        }
    }

    public record ScorePresentation(double score, String explanation, List<CustomerScoreSummary.CycleScore> cycles) {
    }

    private Map<Long, List<TransactionRepository.CustomerCycleProjection>> fetchCycleStats(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<TransactionRepository.CustomerCycleProjection> stats = transactionRepository.findCustomerCycleStats(List.copyOf(ids));
        Map<Long, List<TransactionRepository.CustomerCycleProjection>> grouped = new HashMap<>();
        for (TransactionRepository.CustomerCycleProjection projection : stats) {
            if (projection.getCustomerId() == null) {
                continue;
            }
            grouped.computeIfAbsent(projection.getCustomerId(), unused -> new ArrayList<>()).add(projection);
        }
        return grouped;
    }

    private CustomerScoreSummary buildSummary(Customer customer,
                                              List<TransactionRepository.CustomerCycleProjection> cycleProjections) {
        List<CustomerScoreSummary.CycleScore> cycleScores = new ArrayList<>();
        if (cycleProjections != null && !cycleProjections.isEmpty()) {
            int counter = 1;
            for (TransactionRepository.CustomerCycleProjection projection : cycleProjections) {
                CustomerScoreCalculator.ScoreInputs inputs = buildInputs(projection);
                CustomerScoreCalculator.ScoreResult result = CustomerScoreCalculator.evaluate(inputs);
                cycleScores.add(new CustomerScoreSummary.CycleScore(
                        counter++,
                        projection.getCycleStartDate(),
                        projection.getCycleEndDate(),
                        result));
            }
        }
        if (cycleScores.isEmpty()) {
            boolean hasOutstandingDebt = customer != null && customer.getDebt() != null && customer.getDebt() > 0;
            CustomerScoreCalculator.ScoreInputs fallbackInputs = new CustomerScoreCalculator.ScoreInputs(
                    0, null, null, null, null, null, null, null, hasOutstandingDebt);
            CustomerScoreCalculator.ScoreResult fallbackSummary = CustomerScoreCalculator.evaluate(fallbackInputs);
            return new CustomerScoreSummary(fallbackSummary.score(), Collections.emptyList());
        }
        DoubleSummaryStatistics stats = cycleScores.stream()
                .mapToDouble(cycle -> cycle.result().score())
                .summaryStatistics();
        double roundedAverage = roundTwoDecimals(stats.getAverage());
        return new CustomerScoreSummary(roundedAverage, cycleScores);
    }

    private CustomerScoreCalculator.ScoreInputs buildInputs(TransactionRepository.CustomerCycleProjection projection) {
        return new CustomerScoreCalculator.ScoreInputs(
                projection != null && projection.getTotalPayments() != null ? projection.getTotalPayments() : 0,
                projection != null ? projection.getLastPaymentDate() : null,
                projection != null ? projection.getMaxIntervalBetweenPayments() : null,
                projection != null ? projection.getTotalIntervalDays() : null,
                projection != null ? projection.getIntervalCount() : null,
                projection != null ? projection.getLateIntervalCount() : null,
                projection != null ? projection.getPaymentMonthCount() : null,
                projection != null ? projection.getCycleMonthCount() : null,
                projection != null && Boolean.TRUE.equals(projection.getHasOutstandingDebt())
        );
    }

    private static double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
