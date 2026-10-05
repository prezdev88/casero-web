package cl.casero.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.repository.TransactionRepository;
import cl.casero.migration.repository.TransactionRepository.CustomerCycleProjection;
import cl.casero.migration.service.dto.CustomerRankingEntry;
import cl.casero.migration.service.dto.CustomerScoreInput;
import cl.casero.migration.support.ReadModelFixtures;
import cl.casero.migration.util.CustomerScoreCalculator;
import cl.casero.migration.util.CustomerScoreCalculator.ScoreInputs;
import cl.casero.migration.util.CustomerScoreCalculator.ScoreResult;
import cl.casero.migration.util.CustomerScoreSummary;
import cl.casero.migration.util.CustomerScoreSummary.CycleScore;
import cl.casero.migration.web.controller.CustomerRankingController;

@ExtendWith(MockitoExtension.class)
class CustomerRankingServiceTest {

    private static final int PERFECT_PAYMENT_WINDOW_DAYS = 45;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int CUSTOMER_COUNT = 6;
    private static final int TWO_CYCLES = 2;
    private static final int PAGE_SIZE = 2;
    private static final int LAST_PAGE = 2;
    private static final int OUT_OF_RANGE_PAGE = 3;
    private static final int HTTP_OK = 200;
    private static final int CUSTOMER_DEBT = 12000;
    private static final double MAXIMUM_SCORE = 7.0;
    private static final double MINIMUM_SCORE = 1.0;
    private static final long ALPHA_ID = 1L;
    private static final long ZULU_ID = 2L;
    private static final long BETA_ID = 3L;
    private static final long UNNAMED_ID = 4L;
    private static final long TIED_BETA_ID = 5L;
    private static final long MISSING_SCORE_ID = 6L;
    private static final String RANKING_PATH = "/customers/ranking";

    @Mock
    private CustomerRepository customers;

    @Mock
    private CustomerScoreService scores;

    @Mock
    private TransactionRepository transactions;

    private CustomerRankingService ranking;

    @BeforeEach
    void setUp() {
        CustomerScorePresentationService presentations = new CustomerScorePresentationService(scores);
        ranking = new CustomerRankingService(customers, scores, presentations);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void keepsScoreDirectionCycleCountNameOrderingAndStableTies(boolean ascending) {
        List<Customer> enabled = prepareRanking();
        Pageable pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE);

        Page<CustomerRankingEntry> result = ranking.getRanking(pageable, ascending);

        List<CustomerRankingEntry> content = result.getContent();
        List<Long> expectedIds = ascending
                ? List.of(MISSING_SCORE_ID, UNNAMED_ID, BETA_ID, TIED_BETA_ID, ZULU_ID, ALPHA_ID)
                : List.of(UNNAMED_ID, BETA_ID, TIED_BETA_ID, ZULU_ID, ALPHA_ID, MISSING_SCORE_ID);
        assertThat(content).extracting(CustomerRankingEntry::id).isEqualTo(expectedIds);
        CustomerRankingEntry missingScore = content.stream()
                .filter(entry -> entry.id() == MISSING_SCORE_ID)
                .findFirst().orElseThrow();
        Double fallbackScore = missingScore.score();
        int fallbackCycles = missingScore.cycleCount();
        assertThat(fallbackScore).isEqualTo(MINIMUM_SCORE);
        assertThat(fallbackCycles).isZero();
        verify(customers).findAllByEnabledTrue();
        List<CustomerScoreInput> inputs = ReadModelFixtures.scores(enabled);
        verify(scores).calculateScoreSummaries(inputs);
    }

    @Test
    void usesTheDefaultFirstPageWhenPageableIsAbsent() {
        prepareRanking();

        Page<CustomerRankingEntry> result = ranking.getRanking(null, false);

        int pageNumber = result.getNumber();
        int size = result.getSize();
        long total = result.getTotalElements();
        assertThat(pageNumber).isZero();
        assertThat(size).isEqualTo(DEFAULT_PAGE_SIZE);
        assertThat(total).isEqualTo(CUSTOMER_COUNT);
    }

    @Test
    void keepsTheRequestedPageAndItsSortMetadata() {
        prepareRanking();
        Sort sort = Sort.by("name");
        Pageable pageable = PageRequest.of(1, PAGE_SIZE, sort);

        Page<CustomerRankingEntry> result = ranking.getRanking(pageable, false);

        Pageable resultPageable = result.getPageable();
        List<CustomerRankingEntry> content = result.getContent();
        long total = result.getTotalElements();
        assertThat(resultPageable).isEqualTo(pageable);
        assertThat(content).extracting(CustomerRankingEntry::id).containsExactly(TIED_BETA_ID, ZULU_ID);
        assertThat(total).isEqualTo(CUSTOMER_COUNT);
    }

    @Test
    void fallsBackToTheLastPageWhenTheRequestedOffsetEqualsTheTotal() {
        prepareRanking();
        Sort sort = Sort.by("name");
        Pageable pageable = PageRequest.of(OUT_OF_RANGE_PAGE, PAGE_SIZE, sort);

        Page<CustomerRankingEntry> result = ranking.getRanking(pageable, false);

        int number = result.getNumber();
        Sort resultSort = result.getSort();
        List<CustomerRankingEntry> content = result.getContent();
        assertThat(number).isEqualTo(LAST_PAGE);
        assertThat(resultSort).isEqualTo(sort);
        assertThat(content).extracting(CustomerRankingEntry::id).containsExactly(ALPHA_ID, MISSING_SCORE_ID);
    }

    @Test
    void keepsTheRequestedEmptyPageWithoutCalculatingScores() {
        List<Customer> enabled = List.of();
        doReturn(enabled).when(customers).findAllByEnabledTrue();
        Pageable pageable = PageRequest.of(OUT_OF_RANGE_PAGE, PAGE_SIZE);

        Page<CustomerRankingEntry> result = ranking.getRanking(pageable, true);

        Pageable resultPageable = result.getPageable();
        long total = result.getTotalElements();
        List<CustomerRankingEntry> content = result.getContent();
        assertThat(resultPageable).isEqualTo(pageable);
        assertThat(total).isZero();
        assertThat(content).isEmpty();
        verifyNoInteractions(scores);
    }

    @Test
    void rendersTheRealCalculationAndPresentationWithEscapedNamesAndExistingFields() throws Exception {
        MockMvc mvc = prepareRenderedRanking();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(RANKING_PATH);
        request.param("size", "2");

        MvcResult result = mvc.perform(request).andReturn();

        String html = renderedHtml(result);
        assertThat(html).contains("&lt;Ada&gt;", "Zoe", "12.000", "1.00", "/customers/1",
                        "data-score-target=\"ranking-expl-1\"", "id=\"ranking-expl-1\"",
                        "No registra ciclos de compra evaluables, aún no hay historial para analizar.")
                .doesNotContain("No hay clientes disponibles");
        int adaPosition = html.indexOf("&lt;Ada&gt;");
        int zoePosition = html.indexOf("Zoe");
        assertThat(adaPosition).isLessThan(zoePosition);
        TransactionRepository verification = verify(transactions);
        List<Long> matchedIds = anyList();
        int matchedWindow = eq(PERFECT_PAYMENT_WINDOW_DAYS);
        verification.findCustomerCycleStats(matchedIds, matchedWindow);
    }

    @Test
    void rendersPaginationLinksUsingTheRecoveredLastPageAndDirection() throws Exception {
        MockMvc mvc = prepareRenderedRanking();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(RANKING_PATH);
        request.param("page", "20");
        request.param("size", "1");
        request.param("direction", "asc");

        MvcResult result = mvc.perform(request).andReturn();

        String html = renderedHtml(result);
        assertThat(html).contains("Zoe", "2 / 2", "page=0&amp;size=1&amp;direction=asc",
                        "page=0&amp;size=1&amp;direction=desc")
                .doesNotContain("&lt;Ada&gt;");
    }

    @Test
    void rendersTheExistingEmptyRankingState() throws Exception {
        List<Customer> enabled = List.of();
        doReturn(enabled).when(customers).findAllByEnabledTrue();
        MockMvc mvc = createMvc(ranking);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(RANKING_PATH);

        MvcResult result = mvc.perform(request).andReturn();

        String html = renderedHtml(result);
        assertThat(html).contains("No hay clientes disponibles", "0 / 0");
        verifyNoInteractions(scores, transactions);
    }

    private List<Customer> prepareRanking() {
        Customer alpha = customer(ALPHA_ID, "Alpha", 0);
        Customer zulu = customer(ZULU_ID, "Zulu", 0);
        Customer beta = customer(BETA_ID, "beta", 0);
        Customer unnamed = customer(UNNAMED_ID, null, 0);
        Customer tiedBeta = customer(TIED_BETA_ID, "BETA", 0);
        Customer missing = customer(MISSING_SCORE_ID, "Unknown", null);
        List<Customer> enabled = List.of(alpha, zulu, beta, unnamed, tiedBeta, missing);
        CustomerScoreSummary oneCycle = summary(1);
        CustomerScoreSummary twoCycles = summary(TWO_CYCLES);
        Map<Long, CustomerScoreSummary> summaries = Map.of(
                ALPHA_ID, oneCycle, ZULU_ID, twoCycles, BETA_ID, twoCycles,
                UNNAMED_ID, twoCycles, TIED_BETA_ID, twoCycles);
        doReturn(enabled).when(customers).findAllByEnabledTrue();
        List<CustomerScoreInput> inputs = ReadModelFixtures.scores(enabled);
        doReturn(summaries).when(scores).calculateScoreSummaries(inputs);
        return enabled;
    }

    private CustomerScoreSummary summary(int cycleCount) {
        ScoreInputs inputs = new ScoreInputs(1, null, null, null, null, null, null, null, false);
        ScoreResult evaluation = CustomerScoreCalculator.evaluate(inputs);
        List<CycleScore> cycles = new ArrayList<>();
        for (int cycleNumber = 1; cycleNumber <= cycleCount; cycleNumber++) {
            CycleScore cycle = new CycleScore(cycleNumber, null, null, evaluation);
            cycles.add(cycle);
        }

        return new CustomerScoreSummary(MAXIMUM_SCORE, cycles);
    }

    private Customer customer(long id, String name, Integer debt) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName(name);
        customer.setDebt(debt);
        return customer;
    }

    private MockMvc prepareRenderedRanking() {
        Customer ada = customer(ALPHA_ID, "<Ada>", CUSTOMER_DEBT);
        Customer zoe = customer(ZULU_ID, "Zoe", null);
        List<Customer> enabled = List.of(zoe, ada);
        doReturn(enabled).when(customers).findAllByEnabledTrue();
        List<CustomerCycleProjection> rows = List.of();
        TransactionRepository stub = doReturn(rows).when(transactions);
        List<Long> matchedIds = anyList();
        int matchedWindow = eq(PERFECT_PAYMENT_WINDOW_DAYS);
        stub.findCustomerCycleStats(matchedIds, matchedWindow);
        CustomerScoreService realScores = new CustomerScoreService(transactions);
        CustomerScorePresentationService presentations = new CustomerScorePresentationService(realScores);
        CustomerRankingService realRanking = new CustomerRankingService(customers, realScores, presentations);
        return createMvc(realRanking);
    }

    private MockMvc createMvc(CustomerRankingService rankingService) {
        CustomerRankingController controller = new CustomerRankingController(rankingService);
        String encoding = StandardCharsets.UTF_8.name();
        ClassLoaderTemplateResolver templates = new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/");
        templates.setSuffix(".html");
        templates.setCharacterEncoding(encoding);
        templates.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templates);
        ThymeleafViewResolver views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        views.setCharacterEncoding(encoding);
        StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(controller);
        builder.setViewResolvers(views);
        return builder.build();
    }

    private String renderedHtml(MvcResult result) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        int status = response.getStatus();
        assertThat(status).isEqualTo(HTTP_OK);
        return response.getContentAsString();
    }
}
