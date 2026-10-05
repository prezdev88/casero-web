package cl.casero.migration.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import cl.casero.migration.domain.MonthlyStatistic;
import cl.casero.migration.repository.TransactionRepository;
import cl.casero.migration.repository.TransactionRepository.TopCustomerProjection;
import cl.casero.migration.service.CustomerQueries;
import cl.casero.migration.service.StatisticsService;
import cl.casero.migration.service.dto.OverdueCustomerSummary;
import cl.casero.migration.service.dto.TopCustomerSummary;
import cl.casero.migration.service.impl.TransactionQueryService;

@ExtendWith(MockitoExtension.class)
class DashboardTopCustomersTest {

    private static final String DASHBOARD_PATH = "/dashboard";
    private static final String DASHBOARD_VIEW = "dashboard/index";
    private static final int HTTP_OK = 200;
    private static final int FIRST_CUSTOMER_TOTAL = 12000;
    private static final int SECOND_CUSTOMER_TOTAL = 9000;
    private static final int THIRD_CUSTOMER_TOTAL = 2000;
    private static final Instant REFERENCE_INSTANT = Instant.parse("2024-02-15T12:00:00Z");
    private static final LocalDate MONTH_START = LocalDate.parse("2024-02-01");
    private static final LocalDate MONTH_END = LocalDate.parse("2024-02-29");

    @Mock
    private TransactionRepository repository;

    @Mock
    private CustomerQueries customers;

    @Mock
    private StatisticsService statistics;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(REFERENCE_INSTANT, ZoneOffset.UTC);
        TransactionQueryService queries = new TransactionQueryService(repository, clock);
        DashboardController controller = new DashboardController(customers, statistics, queries);
        Pageable overduePageable = PageRequest.of(0, 1);
        Page<OverdueCustomerSummary> overduePage = Page.empty(overduePageable);
        doReturn(overduePage).when(customers).getOverdueCustomers(overduePageable, 1);
        MonthlyStatistic monthly = new MonthlyStatistic();
        StatisticsService stub = doReturn(monthly).when(statistics);
        int month = anyInt();
        int year = anyInt();
        stub.getMonthlyStatistic(month, year);
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
        mockMvc = builder.build();
    }

    @Test
    void rendersApplicationResultsWithTheSameMonthOrderAmountsAndEscapedNames() throws Exception {
        TopCustomerProjection first = new TopCustomerRow("<First Customer>", FIRST_CUSTOMER_TOTAL);
        TopCustomerProjection second = new TopCustomerRow("Second Customer", SECOND_CUSTOMER_TOTAL);
        TopCustomerProjection third = new TopCustomerRow("Third Customer", THIRD_CUSTOMER_TOTAL);
        List<TopCustomerProjection> rows = List.of(first, second, third);
        doReturn(rows).when(repository).findTopCustomers(MONTH_START, MONTH_END);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(DASHBOARD_PATH);

        MvcResult result = mockMvc.perform(request).andReturn();

        List<?> content = assertResult(result);
        TopCustomerSummary expectedFirst = new TopCustomerSummary("<First Customer>", FIRST_CUSTOMER_TOTAL);
        TopCustomerSummary expectedSecond = new TopCustomerSummary("Second Customer", SECOND_CUSTOMER_TOTAL);
        TopCustomerSummary expectedThird = new TopCustomerSummary("Third Customer", THIRD_CUSTOMER_TOTAL);
        List<TopCustomerSummary> expected = List.of(expectedFirst, expectedSecond, expectedThird);
        assertThat(content).isEqualTo(expected);
        MockHttpServletResponse response = result.getResponse();
        String html = response.getContentAsString();
        assertThat(html).contains("&lt;First Customer&gt;", "Second Customer", "Third Customer", "12.000", "9.000", "2.000")
                .doesNotContain("No hay abonos registrados este mes aún.");
        int firstPosition = html.indexOf("&lt;First Customer&gt;");
        int secondPosition = html.indexOf("Second Customer");
        int thirdPosition = html.indexOf("Third Customer");
        assertThat(firstPosition).isLessThan(secondPosition);
        assertThat(secondPosition).isLessThan(thirdPosition);
        verify(repository).findTopCustomers(MONTH_START, MONTH_END);
    }

    @Test
    void rendersTheExistingEmptyStateWhenTheMonthHasNoPayments() throws Exception {
        List<TopCustomerProjection> rows = List.of();
        doReturn(rows).when(repository).findTopCustomers(MONTH_START, MONTH_END);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(DASHBOARD_PATH);

        MvcResult result = mockMvc.perform(request).andReturn();

        List<?> content = assertResult(result);
        assertThat(content).isEmpty();
        MockHttpServletResponse response = result.getResponse();
        String html = response.getContentAsString();
        assertThat(html).contains("No hay abonos registrados este mes aún.");
        verify(repository).findTopCustomers(MONTH_START, MONTH_END);
    }

    private List<?> assertResult(MvcResult result) {
        MockHttpServletResponse response = result.getResponse();
        int status = response.getStatus();
        assertThat(status).isEqualTo(HTTP_OK);
        ModelAndView view = result.getModelAndView();
        assertThat(view).isNotNull();
        String name = view.getViewName();
        assertThat(name).isEqualTo(DASHBOARD_VIEW);
        Map<String, Object> model = view.getModel();
        return (List<?>) model.get("topCustomers");
    }

    private record TopCustomerRow(String customerName, Integer totalPaid) implements TopCustomerProjection {

        @Override
        public String getCustomerName() {
            return customerName;
        }

        @Override
        public Integer getTotalPaid() {
            return totalPaid;
        }
    }
}
