package cl.casero.migration.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.service.dto.CustomerRankingEntry;
import cl.casero.migration.service.dto.CustomerScorePresentation;
import cl.casero.migration.util.CustomerScoreSummary;

@Service
@RequiredArgsConstructor
public class CustomerRankingService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final CustomerRepository customerRepository;
    private final CustomerScoreService customerScoreService;
    private final CustomerScorePresentationService presentationService;

    public Page<CustomerRankingEntry> getRanking(Pageable pageable, boolean ascending) {
        Pageable effectivePageable = (pageable == null) ? PageRequest.of(0, DEFAULT_PAGE_SIZE) : pageable;
        List<Customer> customers = customerRepository.findAllByEnabledTrue();
        if (customers.isEmpty()) {
            List<CustomerRankingEntry> emptyRanking = List.of();
            return new PageImpl<>(emptyRanking, effectivePageable, 0);
        }

        Map<Long, CustomerScoreSummary> summaries = customerScoreService.calculateScoreSummaries(customers);
        List<CustomerRankingEntry> ranking = new ArrayList<>();
        for (Customer customer : customers) {
            Long customerId = customer.getId();
            CustomerScoreSummary summary = summaries.get(customerId);
            CustomerRankingEntry entry = buildEntry(customer, summary);
            ranking.add(entry);
        }

        Comparator<CustomerRankingEntry> comparator = buildRankingComparator(ascending);
        ranking.sort(comparator);
        return paginate(ranking, effectivePageable);
    }

    private CustomerRankingEntry buildEntry(Customer customer, CustomerScoreSummary summary) {
        CustomerScorePresentation presentation = presentationService.present(summary);
        Long customerId = customer.getId();
        String name = customer.getName();
        Integer debt = customer.getDebt();
        double score = presentation.score();
        String explanation = presentation.explanation();
        int cycleCount = presentation.cycles().size();
        return new CustomerRankingEntry(customerId, name, debt, score, explanation, cycleCount);
    }

    private Comparator<CustomerRankingEntry> buildRankingComparator(boolean ascending) {
        Comparator<CustomerRankingEntry> scoreOrder = Comparator.comparing(CustomerRankingEntry::score);
        if (!ascending) {
            scoreOrder = scoreOrder.reversed();
        }

        Comparator<CustomerRankingEntry> cycleOrder = Comparator.comparingInt(CustomerRankingEntry::cycleCount);
        cycleOrder = cycleOrder.reversed();
        return scoreOrder.thenComparing(cycleOrder)
                .thenComparing(this::getSortingName, String.CASE_INSENSITIVE_ORDER);
    }

    private String getSortingName(CustomerRankingEntry entry) {
        String name = entry.name();
        return (name == null) ? "" : name;
    }

    private Page<CustomerRankingEntry> paginate(List<CustomerRankingEntry> ranking, Pageable pageable) {
        int total = ranking.size();
        int start = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();
        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        Pageable resultPageable = pageable;
        if (start >= total) {
            int lastPage = (total - 1) / pageSize;
            start = lastPage * pageSize;
            Sort sort = pageable.getSort();
            resultPageable = PageRequest.of(lastPage, pageSize, sort);
        }

        int end = Math.min(start + pageSize, total);
        List<CustomerRankingEntry> content = ranking.subList(start, end);
        return new PageImpl<>(content, resultPageable, total);
    }
}
