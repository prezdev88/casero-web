package cl.casero.migration.service;

import java.time.LocalDate;
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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.repository.TransactionRepository;
import cl.casero.migration.repository.TransactionRepository.CustomerCycleProjection;
import cl.casero.migration.util.CustomerScoreCalculator;
import cl.casero.migration.util.CustomerScoreSummary;

@Service
@RequiredArgsConstructor
public class CustomerScoreService {

    private static final double SCORE_DECIMAL_FACTOR = 100.0;

    private final TransactionRepository transactionRepository;

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

        Map<Long, List<CustomerCycleProjection>> cycleStats = fetchCycleStats(ids);
        Map<Long, CustomerScoreSummary> summaries = new HashMap<>();

        List<CustomerCycleProjection> emptyCycles = Collections.emptyList();
        for (Customer customer : customers) {
            if (customer == null) {
                continue;
            }

            Long id = customer.getId();

            if (id == null) {
                continue;
            }

            List<CustomerCycleProjection> cycles = cycleStats.getOrDefault(id, emptyCycles);
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

        List<Customer> singleCustomer = List.of(customer);
        Map<Long, CustomerScoreSummary> summaries = calculateScoreSummaries(singleCustomer);
        Long customerId = customer.getId();
        CustomerScoreSummary summary = summaries.get(customerId);

        return summary != null ? summary.score() : CustomerScoreCalculator.minScore();
    }

    private Map<Long, List<CustomerCycleProjection>> fetchCycleStats(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> customerIds = List.copyOf(ids);
        int paymentWindowDays = CustomerScoreCalculator.perfectPaymentWindowDays();
        List<CustomerCycleProjection> stats = transactionRepository.findCustomerCycleStats(customerIds, paymentWindowDays);
        Map<Long, List<CustomerCycleProjection>> grouped = new HashMap<>();

        for (CustomerCycleProjection projection : stats) {
            Long customerId = projection.getCustomerId();
            if (customerId == null) {
                continue;
            }

            grouped.computeIfAbsent(customerId, unused -> new ArrayList<>()).add(projection);
        }

        return grouped;
    }

    private CustomerScoreSummary buildSummary(
        Customer customer,
        List<CustomerCycleProjection> cycleProjections
    ) {
        List<CustomerScoreSummary.CycleScore> cycleScores = new ArrayList<>();

        if (cycleProjections != null && !cycleProjections.isEmpty()) {
            int counter = 1;
            for (CustomerCycleProjection projection : cycleProjections) {
                CustomerScoreCalculator.ScoreInputs inputs = buildInputs(projection);
                CustomerScoreCalculator.ScoreResult result = CustomerScoreCalculator.evaluate(inputs);
                LocalDate startDate = projection.getCycleStartDate();
                LocalDate endDate = projection.getCycleEndDate();
                CustomerScoreSummary.CycleScore cycleScore = new CustomerScoreSummary.CycleScore(
                        counter++, startDate, endDate, result);
                cycleScores.add(cycleScore);
            }
        }

        if (cycleScores.isEmpty()) {
            boolean hasOutstandingDebt = customer != null && customer.getDebt() != null && customer.getDebt() > 0;
            CustomerScoreCalculator.ScoreInputs fallbackInputs = new CustomerScoreCalculator.ScoreInputs(
                    0, null, null, null, null, null, null, null, hasOutstandingDebt);
            CustomerScoreCalculator.ScoreResult fallbackSummary = CustomerScoreCalculator.evaluate(fallbackInputs);

            double score = fallbackSummary.score();
            List<CustomerScoreSummary.CycleScore> emptyCycles = Collections.emptyList();
            return new CustomerScoreSummary(score, emptyCycles);
        }

        DoubleSummaryStatistics stats = cycleScores.stream()
                .mapToDouble(cycle -> cycle.result().score())
                .summaryStatistics();
        double average = stats.getAverage();
        double roundedAverage = roundTwoDecimals(average);

        return new CustomerScoreSummary(roundedAverage, cycleScores);
    }

    private CustomerScoreCalculator.ScoreInputs buildInputs(CustomerCycleProjection projection) {
        if (projection == null) {
            return new CustomerScoreCalculator.ScoreInputs(
                    0, null, null, null, null, null, null, null, false);
        }

        Integer paymentCount = projection.getTotalPayments();
        int totalPayments = (paymentCount == null) ? 0 : paymentCount;
        LocalDate lastPaymentDate = projection.getLastPaymentDate();
        Integer maxInterval = projection.getMaxIntervalBetweenPayments();
        Long totalIntervalDays = projection.getTotalIntervalDays();
        Integer intervalCount = projection.getIntervalCount();
        Integer lateIntervalCount = projection.getLateIntervalCount();
        Long paymentMonthCount = projection.getPaymentMonthCount();
        Integer cycleMonthCount = projection.getCycleMonthCount();
        Boolean outstandingDebt = projection.getHasOutstandingDebt();
        boolean hasOutstandingDebt = Boolean.TRUE.equals(outstandingDebt);

        return new CustomerScoreCalculator.ScoreInputs(
                totalPayments, lastPaymentDate, maxInterval, totalIntervalDays,
                intervalCount, lateIntervalCount, paymentMonthCount, cycleMonthCount,
                hasOutstandingDebt);
    }

    private static double roundTwoDecimals(double value) {
        return Math.round(value * SCORE_DECIMAL_FACTOR) / SCORE_DECIMAL_FACTOR;
    }
}
