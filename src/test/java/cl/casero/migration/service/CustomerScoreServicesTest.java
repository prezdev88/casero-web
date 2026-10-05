package cl.casero.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.repository.TransactionRepository;
import cl.casero.migration.repository.TransactionRepository.CustomerCycleProjection;
import cl.casero.migration.service.dto.CustomerScorePresentation;
import cl.casero.migration.util.CustomerScoreSummary;
import cl.casero.migration.util.CustomerScoreSummary.CycleScore;

@ExtendWith(MockitoExtension.class)
class CustomerScoreServicesTest {

    private static final long CUSTOMER_ID = 7L;
    private static final long OTHER_CUSTOMER_ID = 8L;
    private static final double MINIMUM_SCORE = 1.0;
    private static final double MAXIMUM_SCORE = 7.0;
    private static final double AVERAGE_SCORE = 4.0;
    private static final int CYCLE_COUNT = 2;
    private static final LocalDate FIRST_CYCLE_DATE = LocalDate.parse("2024-01-01");
    private static final LocalDate SECOND_CYCLE_DATE = LocalDate.parse("2024-02-01");
    private static final String NO_HISTORY_EXPLANATION =
            "No registra ciclos de compra evaluables, aún no hay historial para analizar.";

    @Mock
    private TransactionRepository repository;

    @Captor
    private ArgumentCaptor<List<Long>> customerIds;

    private CustomerScoreService scores;
    private CustomerScorePresentationService presentations;

    @BeforeEach
    void setUp() {
        scores = new CustomerScoreService(repository);
        presentations = new CustomerScorePresentationService(scores);
    }

    @Test
    void returnsEmptyScoresForNullAndEmptyCollectionsWithoutQuerying() {
        Map<Long, CustomerScoreSummary> nullSummaries = scores.calculateScoreSummaries(null);
        List<Customer> emptyCustomers = List.of();
        Map<Long, Double> emptyScores = scores.calculateScores(emptyCustomers);

        assertThat(nullSummaries).isEmpty();
        assertThat(emptyScores).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void preservesTheDistinctPresentationForAnAbsentCustomer() {
        double score = scores.calculateScore(null);
        CustomerScorePresentation presentation = presentations.getScorePresentation(null);
        double presentedScore = presentation.score();
        String explanation = presentation.explanation();
        List<CycleScore> cycles = presentation.cycles();

        assertThat(score).isEqualTo(MINIMUM_SCORE);
        assertThat(presentedScore).isEqualTo(MINIMUM_SCORE);
        assertThat(explanation).isEmpty();
        assertThat(cycles).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void presentsAnUnpersistedCustomerAsHavingNoEvaluableHistory() {
        Customer customer = new Customer();
        CustomerScorePresentation presentation = presentations.getScorePresentation(customer);
        double score = presentation.score();
        String explanation = presentation.explanation();
        List<CycleScore> cycles = presentation.cycles();

        assertThat(score).isEqualTo(MINIMUM_SCORE);
        assertThat(explanation).isEqualTo(NO_HISTORY_EXPLANATION);
        assertThat(cycles).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void keepsMinimumScoreAndExplanationWhenTheCustomerHasNoCycles() {
        Customer customer = customer(CUSTOMER_ID);
        List<Long> ids = List.of(CUSTOMER_ID);
        List<CustomerCycleProjection> rows = List.of();
        doReturn(rows).when(repository).findCustomerCycleStats(ids);

        CustomerScorePresentation presentation = presentations.getScorePresentation(customer);
        double score = presentation.score();
        String explanation = presentation.explanation();
        List<CycleScore> cycles = presentation.cycles();

        assertThat(score).isEqualTo(MINIMUM_SCORE);
        assertThat(explanation).isEqualTo(NO_HISTORY_EXPLANATION);
        assertThat(cycles).isEmpty();
        verify(repository).findCustomerCycleStats(ids);
    }

    @Test
    void groupsOneBatchOfCyclesAndKeepsEvaluationAverageDatesAndCycleOrder() {
        Customer first = customer(CUSTOMER_ID);
        Customer second = customer(OTHER_CUSTOMER_ID);
        List<Customer> customers = List.of(first, second);
        CustomerCycleProjection paid = projection(CUSTOMER_ID, 1, FIRST_CYCLE_DATE);
        CustomerCycleProjection other = projection(OTHER_CUSTOMER_ID, 1, FIRST_CYCLE_DATE);
        CustomerCycleProjection unpaid = projection(CUSTOMER_ID, 0, SECOND_CYCLE_DATE);
        CustomerCycleProjection unidentified = mock(CustomerCycleProjection.class);
        List<CustomerCycleProjection> rows = List.of(paid, other, unidentified, unpaid);
        TransactionRepository stub = doReturn(rows).when(repository);
        List<Long> matchedIds = customerIds.capture();
        stub.findCustomerCycleStats(matchedIds);

        Map<Long, CustomerScoreSummary> summaries = scores.calculateScoreSummaries(customers);

        assertThat(summaries).containsOnlyKeys(CUSTOMER_ID, OTHER_CUSTOMER_ID);
        CustomerScoreSummary firstSummary = summaries.get(CUSTOMER_ID);
        CustomerScoreSummary secondSummary = summaries.get(OTHER_CUSTOMER_ID);
        double firstScore = firstSummary.score();
        double secondScore = secondSummary.score();
        List<CycleScore> cycles = firstSummary.cycles();
        assertThat(firstScore).isEqualTo(AVERAGE_SCORE);
        assertThat(secondScore).isEqualTo(MAXIMUM_SCORE);
        assertThat(cycles).hasSize(CYCLE_COUNT);
        assertThat(cycles).extracting(CycleScore::cycleNumber).containsExactly(1, CYCLE_COUNT);
        assertThat(cycles).extracting(CycleScore::cycleStartDate)
                .containsExactly(FIRST_CYCLE_DATE, SECOND_CYCLE_DATE);
        assertThat(cycles).extracting(CycleScore::cycleEndDate)
                .containsExactly(FIRST_CYCLE_DATE, SECOND_CYCLE_DATE);
        CycleScore firstCycle = cycles.get(0);
        CycleScore secondCycle = cycles.get(1);
        double paidScore = firstCycle.result().score();
        double unpaidScore = secondCycle.result().score();
        assertThat(paidScore).isEqualTo(MAXIMUM_SCORE);
        assertThat(unpaidScore).isEqualTo(MINIMUM_SCORE);
        TransactionRepository verification = verify(repository);
        List<Long> queriedIds = customerIds.capture();
        verification.findCustomerCycleStats(queriedIds);
        List<Long> capturedIds = customerIds.getValue();
        assertThat(capturedIds).containsExactlyInAnyOrder(CUSTOMER_ID, OTHER_CUSTOMER_ID);
    }

    @Test
    void preservesNarrativeAndLatestCycleFirstWithoutQueryingAgain() {
        CustomerCycleProjection paid = projection(CUSTOMER_ID, 1, FIRST_CYCLE_DATE);
        CustomerCycleProjection unpaid = projection(CUSTOMER_ID, 0, SECOND_CYCLE_DATE);
        List<CustomerCycleProjection> rows = List.of(paid, unpaid);
        List<Long> ids = List.of(CUSTOMER_ID);
        doReturn(rows).when(repository).findCustomerCycleStats(ids);
        Customer customer = customer(CUSTOMER_ID);
        List<Customer> customers = List.of(customer);
        Map<Long, CustomerScoreSummary> summaries = scores.calculateScoreSummaries(customers);
        CustomerScoreSummary summary = summaries.get(CUSTOMER_ID);

        CustomerScorePresentation presentation = presentations.present(summary);

        double score = presentation.score();
        String explanation = presentation.explanation();
        List<CycleScore> cycles = presentation.cycles();
        List<CycleScore> originalCycles = summary.cycles();
        assertThat(score).isEqualTo(AVERAGE_SCORE);
        assertThat(cycles).isSameAs(originalCycles);
        assertThat(explanation).startsWith("Se analizaron 2 ciclos.")
                .contains("nota 7.00, historial parcial de pagos y dejó el ciclo al día.",
                        "nota 1.00, sin pagos registrados y dejó el ciclo al día.");
        int latestPosition = explanation.indexOf("Ciclo 2");
        int firstPosition = explanation.indexOf("Ciclo 1");
        assertThat(latestPosition).isLessThan(firstPosition);
        verify(repository).findCustomerCycleStats(ids);
    }

    private Customer customer(long id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setDebt(0);
        return customer;
    }

    private CustomerCycleProjection projection(long customerId, int payments, LocalDate date) {
        CustomerCycleProjection projection = mock(CustomerCycleProjection.class);
        doReturn(customerId).when(projection).getCustomerId();
        doReturn(payments).when(projection).getTotalPayments();
        doReturn(date).when(projection).getCycleStartDate();
        doReturn(date).when(projection).getCycleEndDate();
        return projection;
    }
}
