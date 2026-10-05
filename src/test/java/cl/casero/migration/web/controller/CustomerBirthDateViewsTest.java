package cl.casero.migration.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
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
import cl.casero.migration.domain.Sector;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.service.CustomerQueries;
import cl.casero.migration.service.CustomerScorePresentationService;
import cl.casero.migration.service.CustomerScoreService;
import cl.casero.migration.service.TransactionQueries;
import cl.casero.migration.service.dto.CustomerScorePresentation;
import cl.casero.migration.util.CustomerScoreSummary.CycleScore;
import cl.casero.migration.web.presentation.CustomerBirthDateFormatter;

@ExtendWith(MockitoExtension.class)
class CustomerBirthDateViewsTest {

    private static final long CUSTOMER_ID = 7L;
    private static final long OTHER_CUSTOMER_ID = 8L;
    private static final int PAGE_SIZE = 10;
    private static final int BIRTH_DAY = 31;
    private static final int BIRTH_MONTH = 12;
    private static final int BIRTH_YEAR = 2000;
    private static final double SCORE = 1.0;
    private static final int HTTP_OK = 200;
    private static final String BIRTH_DATE_LABEL = "31 de Diciembre";

    @Mock
    private CustomerQueries queries;

    @Mock
    private CustomerScoreService scores;

    @Mock
    private CustomerScorePresentationService presentations;

    @Mock
    private TransactionQueries transactions;

    private MockMvc mvc;
    private Customer datedCustomer;
    private Customer undatedCustomer;

    @BeforeEach
    void setUp() {
        datedCustomer = customer(CUSTOMER_ID, "<Dated Customer>");
        datedCustomer.setBirthDay(BIRTH_DAY);
        datedCustomer.setBirthMonth(BIRTH_MONTH);
        undatedCustomer = customer(OTHER_CUSTOMER_ID, "Undated Customer");
        CustomerBirthDateFormatter formatter = new CustomerBirthDateFormatter();
        CustomerController controller = new CustomerController(queries, scores, presentations, transactions, formatter);
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
        mvc = builder.build();
    }

    @Test
    void rendersListBirthDateDataWithAnAbsentDateAndEscapedNames() throws Exception {
        prepareSearch();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/customers");
        request.param("q", "Customer");

        MvcResult result = mvc.perform(request).andReturn();

        String html = responseBody(result);
        assertThat(html).contains("&lt;Dated Customer&gt;", "Undated Customer",
                "data-customer-birthdate=\"31 de Diciembre\"", "data-customer-id=\"8\"");
    }

    @Test
    void keepsTheJsonBirthDatePropertyAndNullForAnAbsentDate() throws Exception {
        prepareSearch();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/customers");
        request.param("q", "Customer");
        request.accept(MediaType.APPLICATION_JSON);

        MvcResult result = mvc.perform(request).andReturn();

        String body = responseBody(result);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(body);
        JsonNode content = json.path("content");
        JsonNode first = content.get(0);
        JsonNode second = content.get(1);
        JsonNode firstBirthDate = first.path("formattedBirthDate");
        JsonNode secondBirthDate = second.path("formattedBirthDate");
        String label = firstBirthDate.asText();
        boolean nullBirthDate = secondBirthDate.isNull();
        assertThat(label).isEqualTo(BIRTH_DATE_LABEL);
        assertThat(nullBirthDate).isTrue();
    }

    @Test
    void rendersDetailWithTheYearAndCurrentAgePreparedOutsideTheDomain() throws Exception {
        datedCustomer.setBirthYear(BIRTH_YEAR);
        prepareDetail(datedCustomer);
        LocalDate today = LocalDate.now();
        int referenceYear = today.getYear();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();
        boolean birthdayPassed = month == BIRTH_MONTH && day == BIRTH_DAY;
        int expectedAge = referenceYear - BIRTH_YEAR - (birthdayPassed ? 0 : 1);
        String expected = BIRTH_DATE_LABEL + " de " + BIRTH_YEAR + " (" + expectedAge + " años)";
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/customers/{id}", CUSTOMER_ID);

        MvcResult result = mvc.perform(request).andReturn();

        String html = responseBody(result);
        assertThat(html).contains(expected, "&lt;Dated Customer&gt;")
                .doesNotContain("Añadir fecha de nacimiento");
    }

    @Test
    void keepsTheAddBirthDateActionWhenTheCustomerHasNoDate() throws Exception {
        prepareDetail(undatedCustomer);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/customers/{id}", OTHER_CUSTOMER_ID);

        MvcResult result = mvc.perform(request).andReturn();

        String html = responseBody(result);
        assertThat(html).contains("Añadir fecha de nacimiento", "/customers/8/actions/birthdate/edit")
                .doesNotContain(BIRTH_DATE_LABEL);
    }

    private Customer customer(long id, String name) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName(name);
        customer.setAddress("Test Address");
        Sector sector = new Sector();
        sector.setName("Central");
        customer.setSector(sector);
        return customer;
    }

    private void prepareSearch() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        List<Customer> customers = List.of(datedCustomer, undatedCustomer);
        int customerCount = customers.size();
        Page<Customer> page = new PageImpl<>(customers, pageable, customerCount);
        Map<Long, Double> scoreResults = Map.of(CUSTOMER_ID, SCORE, OTHER_CUSTOMER_ID, SCORE);
        doReturn(page).when(queries).search("Customer", pageable);
        doReturn(scoreResults).when(scores).calculateScores(customers);
    }

    private void prepareDetail(Customer customer) {
        Long id = customer.getId();
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, sort);
        Page<Transaction> page = Page.empty(pageable);
        List<CycleScore> cycles = List.of();
        CustomerScorePresentation presentation = new CustomerScorePresentation(SCORE, "", cycles);
        doReturn(customer).when(queries).get(id);
        doReturn(presentation).when(presentations).getScorePresentation(customer);
        doReturn(page).when(transactions).listByCustomer(id, pageable);
    }

    private String responseBody(MvcResult result) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        int status = response.getStatus();
        assertThat(status).isEqualTo(HTTP_OK);
        return response.getContentAsString();
    }
}
