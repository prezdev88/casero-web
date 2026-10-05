package cl.casero.migration.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

import cl.casero.migration.repository.CustomerRepository;
import cl.casero.migration.repository.CustomerRepository.SectorCountView;
import cl.casero.migration.service.SectorService;
import cl.casero.migration.service.StatisticsService;
import cl.casero.migration.service.dto.SectorCustomerCount;
import cl.casero.migration.service.impl.CustomerServiceImpl;

@ExtendWith(MockitoExtension.class)
class SectorStatisticsTest {

    private static final String SECTORS_PATH = "/statistics/sectors";
    private static final String SECTORS_VIEW = "statistics/sectors";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int PAGED_SIZE = 2;
    private static final long TOTAL_SECTORS = 5;
    private static final long LARGE_CUSTOMER_COUNT = 3_000_000_000L;
    private static final long SOUTH_CUSTOMER_COUNT = 12;
    private static final int HTTP_OK = 200;

    @Mock
    private CustomerRepository repository;

    @Mock
    private SectorService sectors;

    @Mock
    private StatisticsService statistics;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CustomerServiceImpl customerService = new CustomerServiceImpl(sectors, repository);
        StatisticsController controller = new StatisticsController(customerService, statistics);
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
    void rendersApplicationResultsWithPreservedOrderCountsAndPagination() throws Exception {
        Pageable pageable = PageRequest.of(1, PAGED_SIZE);
        SectorCountView central = new SectorCountRow("<Central>", LARGE_CUSTOMER_COUNT);
        SectorCountView south = new SectorCountRow("Zona Sur", SOUTH_CUSTOMER_COUNT);
        List<SectorCountView> rows = List.of(central, south);
        Page<SectorCountView> page = new PageImpl<>(rows, pageable, TOTAL_SECTORS);
        doReturn(page).when(repository).countBySector(pageable);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(SECTORS_PATH);
        request.param("page", "1");
        request.param("size", "2");

        MvcResult result = mockMvc.perform(request).andReturn();

        Page<?> mappedPage = assertResult(result);
        List<?> content = mappedPage.getContent();
        SectorCustomerCount expectedCentral = new SectorCustomerCount("<Central>", LARGE_CUSTOMER_COUNT);
        SectorCustomerCount expectedSouth = new SectorCustomerCount("Zona Sur", SOUTH_CUSTOMER_COUNT);
        List<SectorCustomerCount> expected = List.of(expectedCentral, expectedSouth);
        long total = mappedPage.getTotalElements();
        Pageable actualPageable = mappedPage.getPageable();
        assertThat(content).isEqualTo(expected);
        assertThat(total).isEqualTo(TOTAL_SECTORS);
        assertThat(actualPageable).isEqualTo(pageable);
        MockHttpServletResponse response = result.getResponse();
        String html = response.getContentAsString();
        assertThat(html).contains("&lt;Central&gt;", "Zona Sur", "3000000000", "<td>12</td>",
                "/statistics/sectors?page=0&amp;size=2", "/statistics/sectors?page=2&amp;size=2");
        verify(repository).countBySector(pageable);
        verifyNoInteractions(sectors, statistics);
    }

    @Test
    void rendersTheEmptyStateWithDefaultPagination() throws Exception {
        Pageable pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE);
        Page<SectorCountView> emptyPage = Page.empty(pageable);
        doReturn(emptyPage).when(repository).countBySector(pageable);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(SECTORS_PATH);

        MvcResult result = mockMvc.perform(request).andReturn();

        Page<?> mappedPage = assertResult(result);
        List<?> content = mappedPage.getContent();
        Pageable actualPageable = mappedPage.getPageable();
        long total = mappedPage.getTotalElements();
        assertThat(content).isEmpty();
        assertThat(actualPageable).isEqualTo(pageable);
        assertThat(total).isZero();
        MockHttpServletResponse response = result.getResponse();
        String html = response.getContentAsString();
        assertThat(html).contains("Sin datos").doesNotContain("aria-label=\"Página anterior\"");
        verify(repository).countBySector(pageable);
    }

    @ParameterizedTest
    @CsvSource({"-3, 0, 0, 1", "0, 999, 0, 100", "1, 2, 1, 2"})
    void preservesPaginationBounds(int page, int size, int expectedPage, int expectedSize) throws Exception {
        Pageable pageable = PageRequest.of(expectedPage, expectedSize);
        Page<SectorCountView> emptyPage = Page.empty(pageable);
        doReturn(emptyPage).when(repository).countBySector(pageable);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(SECTORS_PATH);
        String pageValue = Integer.toString(page);
        String sizeValue = Integer.toString(size);
        request.param("page", pageValue);
        request.param("size", sizeValue);

        MvcResult result = mockMvc.perform(request).andReturn();

        Page<?> mappedPage = assertResult(result);
        Pageable actualPageable = mappedPage.getPageable();
        assertThat(actualPageable).isEqualTo(pageable);
        verify(repository).countBySector(pageable);
    }

    private Page<?> assertResult(MvcResult result) {
        MockHttpServletResponse response = result.getResponse();
        int status = response.getStatus();
        assertThat(status).isEqualTo(HTTP_OK);
        ModelAndView view = result.getModelAndView();
        assertThat(view).isNotNull();
        String name = view.getViewName();
        assertThat(name).isEqualTo(SECTORS_VIEW);
        Map<String, Object> model = view.getModel();
        return (Page<?>) model.get("sectorsPage");
    }

    private record SectorCountRow(String name, long total) implements SectorCountView {

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getTotal() {
            return total;
        }
    }
}
