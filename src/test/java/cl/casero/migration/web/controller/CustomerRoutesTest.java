package cl.casero.migration.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Sector;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.domain.enums.UserRole;
import cl.casero.migration.service.AuditEventService;
import cl.casero.migration.service.CustomerCommands;
import cl.casero.migration.service.CustomerQueries;
import cl.casero.migration.service.CustomerRankingService;
import cl.casero.migration.service.CustomerReportService;
import cl.casero.migration.service.CustomerScorePresentationService;
import cl.casero.migration.service.CustomerScoreService;
import cl.casero.migration.service.CustomerTransactionReportService;
import cl.casero.migration.service.SectorService;
import cl.casero.migration.service.StatisticsService;
import cl.casero.migration.service.TransactionCommands;
import cl.casero.migration.service.TransactionQueries;
import cl.casero.migration.service.dto.AuditContext;
import cl.casero.migration.service.dto.CreateCustomerForm;
import cl.casero.migration.service.dto.CustomerRankingEntry;
import cl.casero.migration.service.dto.CustomerScorePresentation;
import cl.casero.migration.util.CustomerScoreCalculator.ScoreResult;
import cl.casero.migration.util.CustomerScoreCalculator;
import cl.casero.migration.util.CustomerScoreSummary.CycleScore;
import cl.casero.migration.web.audit.AuditContextFactory;
import cl.casero.migration.web.audit.CustomerAuditLogger;
import cl.casero.migration.web.security.CaseroUserDetails;

@ExtendWith(MockitoExtension.class)
class CustomerRoutesTest {

    private static final long CUSTOMER_ID = 7L;
    private static final long SECTOR_ID = 3L;
    private static final long TRANSACTION_ID = 11L;
    private static final int PAGE_SIZE = 10;
    private static final int SECOND_CYCLE_NUMBER = 2;
    private static final int CUSTOMER_JSON_FIELD_COUNT = 9;
    private static final int TRANSACTION_JSON_FIELD_COUNT = 6;
    private static final int PAGE_JSON_FIELD_COUNT = 6;
    private static final int UPDATED_PROFILE_EVENT_COUNT = 2;
    private static final int MAX_RANKING_PAGE_SIZE = 100;
    private static final int CUSTOMER_DEBT = 200;
    private static final int PAYMENT_AMOUNT = 100;
    private static final int PROFILE_BIRTH_DAY = 4;
    private static final int PROFILE_BIRTH_MONTH = 10;
    private static final double CUSTOMER_SCORE = 4.0;
    private static final String FORM_DATE = "2026-10-04";
    private static final Instant REPORT_INSTANT = Instant.parse("2026-10-04T12:00:00Z");
    private static final ZoneId CUSTOMER_ZONE = ZoneId.of("America/Santiago");

    @Mock
    private CustomerQueries customerQueries;

    @Mock
    private CustomerCommands customerCommands;

    @Mock
    private CustomerScoreService customerScoreService;

    @Mock
    private CustomerScorePresentationService presentationService;

    @Mock
    private CustomerRankingService customerRankingService;

    @Mock
    private TransactionQueries transactionQueries;

    @Mock
    private TransactionCommands transactionCommands;

    @Mock
    private StatisticsService statisticsService;

    @Mock
    private SectorService sectorService;

    @Mock
    private CustomerReportService customerReportService;

    @Mock
    private AuditEventService auditEventService;

    private MockMvc mockMvc;
    private Customer customer;
    private Clock reportClock;

    @BeforeEach
    void setUp() {
        Sector sector = new Sector();
        sector.setId(SECTOR_ID);
        sector.setName("Central");
        customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setName("Test Customer");
        customer.setAddress("  Test\n Address  ");
        customer.setDebt(CUSTOMER_DEBT);
        customer.setSector(sector);

        AuditContextFactory contextFactory = new AuditContextFactory();
        CustomerAuditLogger auditLogger = new CustomerAuditLogger(auditEventService, contextFactory);
        CustomerController customerViews = new CustomerController(
                customerQueries, customerScoreService, presentationService, transactionQueries);
        CustomerManagementController management = new CustomerManagementController(
                customerQueries, customerCommands, sectorService, statisticsService, auditLogger);
        CustomerTransactionController transactions = new CustomerTransactionController(
                customerQueries, transactionQueries, transactionCommands, auditLogger);
        CustomerRankingController ranking = new CustomerRankingController(customerRankingService);
        reportClock = Clock.fixed(REPORT_INSTANT, ZoneOffset.UTC);
        CustomerTransactionReportService reportPreparation = new CustomerTransactionReportService(
                customerQueries, transactionQueries, reportClock);
        CustomerReportController reports = new CustomerReportController(reportPreparation, customerReportService);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver("/test-views/", ".html");
        StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(
                customerViews, management, transactions, ranking, reports);
        builder.setViewResolvers(viewResolver);
        mockMvc = builder.build();
    }

    @ParameterizedTest
    @CsvSource({
        "payment, customers/actions/payment, paymentForm",
        "sale, customers/actions/sale, saleForm",
        "refund, customers/actions/refund, refundForm",
        "fault-discount, customers/actions/fault-discount, faultDiscountForm",
        "forgiveness, customers/actions/forgiveness, debtForgivenessForm",
        "address/edit, customers/actions/address-edit, updateAddressForm",
        "name/edit, customers/actions/name-edit, updateNameForm",
        "sector/edit, customers/actions/sector-edit, updateSectorForm",
        "birthdate/edit, customers/actions/birthdate-edit, updateBirthdateForm"
    })
    void keepsActionViewsAndPreservedForms(String action, String expectedView, String formAttribute) throws Exception {
        doReturn(customer).when(customerQueries).get(CUSTOMER_ID);
        Object preservedForm = new Object();
        String path = "/customers/" + CUSTOMER_ID + "/actions/" + action;
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(path);
        request.flashAttr(formAttribute, preservedForm);
        MvcResult result = mockMvc.perform(request).andReturn();

        assertStatus(result, HttpStatus.OK);
        ModelAndView modelAndView = result.getModelAndView();
        assertThat(modelAndView).isNotNull();
        String viewName = modelAndView.getViewName();
        Map<String, Object> model = modelAndView.getModel();
        assertThat(viewName).isEqualTo(expectedView);
        assertThat(model).containsEntry("customer", customer).containsEntry(formAttribute, preservedForm);
    }

    @ParameterizedTest
    @CsvSource({
        "sales, saleForm, sale",
        "payments, paymentForm, payment",
        "refunds, refundForm, refund",
        "fault-discounts, faultDiscountForm, fault-discount",
        "forgiveness, debtForgivenessForm, forgiveness",
        "address, updateAddressForm, address/edit",
        "sector, updateSectorForm, sector/edit",
        "name, updateNameForm, name/edit",
        "birthdate, updateBirthdateForm, birthdate/edit"
    })
    void rejectsInvalidFormsWithoutWritingOrAuditing(String route, String formAttribute, String action) throws Exception {
        String path = "/customers/" + CUSTOMER_ID + "/" + route;
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(path);
        MvcResult result = mockMvc.perform(request).andReturn();
        String redirect = "/customers/" + CUSTOMER_ID + "/actions/" + action;

        assertRedirect(result, redirect);
        Map<String, Object> flash = result.getFlashMap();
        String bindingKey = BindingResult.MODEL_KEY_PREFIX + formAttribute;
        assertThat(flash).containsKeys(formAttribute, bindingKey);
        BindingResult validation = (BindingResult) flash.get(bindingKey);
        boolean hasErrors = validation.hasErrors();
        assertThat(hasErrors).isTrue();
        verifyNoInteractions(customerQueries, customerCommands, transactionQueries, transactionCommands, auditEventService);
    }

    @ParameterizedTest
    @CsvSource({
        "sales, SALE",
        "payments, PAYMENT",
        "refunds, REFUND",
        "fault-discounts, FAULT_DISCOUNT",
        "forgiveness, DEBT_FORGIVEN",
        "address, UPDATE_CUSTOMER_ADDRESS",
        "sector, UPDATE_CUSTOMER_SECTOR",
        "delete, DELETE_CUSTOMER"
    })
    void keepsMutationRoutesAndWrappedAuditPayloads(String route, String actionType) throws Exception {
        String path = "/customers/" + CUSTOMER_ID + "/" + route;
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(path);
        request.param("amount", "100");
        request.param("itemsCount", "1");
        request.param("date", FORM_DATE);
        request.param("detail", "  Test\n detail  ");
        request.param("newAddress", "  New\n Address  ");
        String sectorId = Long.toString(SECTOR_ID);
        request.param("sectorId", sectorId);
        MvcResult result = mockMvc.perform(request).andReturn();
        String redirect = route.equals("delete") ? "/customers" : "/customers/" + CUSTOMER_ID;
        assertRedirect(result, redirect);

        if (route.equals("address")) {
            verify(customerCommands).updateAddress(CUSTOMER_ID, "  New\n Address  ");
        } else if (route.equals("sector")) {
            verify(customerCommands).updateSector(CUSTOMER_ID, SECTOR_ID);
        } else if (route.equals("delete")) {
            verify(customerCommands).delete(CUSTOMER_ID);
        }

        verifyNoInteractions(customerQueries);
        Map<String, Object> payload = captureAnonymousAudit();
        assertThat(payload).containsOnlyKeys("type", "data").containsEntry("type", actionType);
        Map<?, ?> data = (Map<?, ?>) payload.get("data");
        Object customerId = data.get("customerId");
        assertThat(customerId).isEqualTo(CUSTOMER_ID);
        if (data.containsKey("detail")) {
            Object detail = data.get("detail");
            assertThat(detail).isEqualTo("Test detail");
        }

        if (data.containsKey("date")) {
            Object date = data.get("date");
            assertThat(date).isEqualTo(FORM_DATE);
        }

        if (data.containsKey("address")) {
            Object address = data.get("address");
            assertThat(address).isEqualTo("New Address");
        }
    }

    @Test
    void keepsFlatNameAndBirthdateAuditFormatsAndOmitsMissingYear() throws Exception {
        MockHttpServletRequestBuilder nameRequest = MockMvcRequestBuilders.post("/customers/{id}/name", CUSTOMER_ID);
        nameRequest.param("newName", "New Name");
        MvcResult nameResult = mockMvc.perform(nameRequest).andReturn();
        String redirect = "/customers/" + CUSTOMER_ID;
        assertRedirect(nameResult, redirect);

        MockHttpServletRequestBuilder birthdateRequest = MockMvcRequestBuilders.post("/customers/{id}/birthdate", CUSTOMER_ID);
        birthdateRequest.param("day", "4");
        birthdateRequest.param("month", "10");
        MvcResult birthdateResult = mockMvc.perform(birthdateRequest).andReturn();
        assertRedirect(birthdateResult, redirect);

        verify(customerCommands).updateName(CUSTOMER_ID, "New Name");
        verify(customerCommands).updateBirthdate(CUSTOMER_ID, PROFILE_BIRTH_DAY, PROFILE_BIRTH_MONTH, null);
        verifyNoInteractions(customerQueries);
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.captor();
        VerificationMode expectedEvents = times(UPDATED_PROFILE_EVENT_COUNT);
        AuditEventService auditVerification = verify(auditEventService, expectedEvents);
        AuditEventType eventType = eq(AuditEventType.ACTION);
        AuditContext anonymousContext = new AuditContext(null, "127.0.0.1", null);
        Map<String, Object> capturedPayload = captor.capture();
        AuditContext expectedContext = eq(anonymousContext);
        auditVerification.logEvent(eventType, capturedPayload, expectedContext);
        List<Map<String, Object>> payloads = captor.getAllValues();
        Map<String, Object> namePayload = payloads.get(0);
        Map<String, Object> birthdayPayload = payloads.get(1);
        assertThat(namePayload).containsOnlyKeys("action", "customerId", "name")
                .containsEntry("action", "UPDATE_CUSTOMER_NAME");
        assertThat(birthdayPayload).containsOnlyKeys("action", "customerId", "day", "month")
                .containsEntry("action", "UPDATE_CUSTOMER_BIRTHDATE");
    }

    @Test
    void createsCustomerAndAuditsTheAuthenticatedActor() throws Exception {
        CustomerCommands creationStub = doReturn(customer).when(customerCommands);
        CreateCustomerForm matchedForm = any(CreateCustomerForm.class);
        creationStub.create(matchedForm);
        AppUser actor = new AppUser();
        actor.setRole(UserRole.NORMAL);
        CaseroUserDetails details = new CaseroUserDetails(actor);
        Authentication authentication = new TestingAuthenticationToken(details, null);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/customers");
        request.principal(authentication);
        request.param("name", "Test Customer");
        request.param("address", "Test Address");
        String sectorId = Long.toString(SECTOR_ID);
        request.param("sectorId", sectorId);
        MvcResult result = mockMvc.perform(request).andReturn();
        assertRedirect(result, "/customers");

        verifyNoInteractions(customerQueries);
        Map<String, Object> data = Map.of("customerId", CUSTOMER_ID, "name", "Test Customer",
                "sectorId", SECTOR_ID, "address", "Test Address");
        Map<String, Object> payload = Map.of("type", "CREATE_CUSTOMER", "data", data);
        AuditEventService auditVerification = verify(auditEventService);
        AuditEventType eventType = eq(AuditEventType.ACTION);
        AuditContext actorContext = new AuditContext(actor, "127.0.0.1", null);
        Map<String, Object> expectedPayload = eq(payload);
        AuditContext expectedContext = eq(actorContext);
        auditVerification.logEvent(eventType, expectedPayload, expectedContext);
    }

    @Test
    void deletesTransactionUsingTheExistingRouteAndPayload() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(
                "/customers/transactions/{id}/delete", TRANSACTION_ID);
        String customerId = Long.toString(CUSTOMER_ID);
        request.param("customerId", customerId);
        MvcResult result = mockMvc.perform(request).andReturn();
        String redirect = "/customers/" + CUSTOMER_ID;
        assertRedirect(result, redirect);

        verify(transactionCommands).delete(TRANSACTION_ID);
        Map<String, Object> payload = captureAnonymousAudit();
        Map<String, Object> data = Map.of("transactionId", TRANSACTION_ID, "customerId", CUSTOMER_ID);
        assertThat(payload).containsEntry("type", "TRANSACTION_DELETED").containsEntry("data", data);
    }

    @Test
    void keepsCustomerJsonFieldsAndBirthdayValues() throws Exception {
        LocalDate today = LocalDate.now(CUSTOMER_ZONE);
        int day = today.getDayOfMonth();
        int month = today.getMonthValue();
        customer.setBirthDay(day);
        customer.setBirthMonth(month);
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        List<Customer> customers = List.of(customer);
        Page<Customer> page = new PageImpl<>(customers, pageable, 1);
        doReturn(page).when(customerQueries).search("Test", pageable);
        Map<Long, Double> scores = Map.of(CUSTOMER_ID, CUSTOMER_SCORE);
        doReturn(scores).when(customerScoreService).calculateScores(customers);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/customers");
        request.param("q", " Test ");
        request.accept(MediaType.APPLICATION_JSON);
        MvcResult result = mockMvc.perform(request).andReturn();
        assertStatus(result, HttpStatus.OK);

        JsonNode json = readJson(result);
        JsonNode content = json.path("content");
        JsonNode entry = content.get(0);
        JsonNode id = entry.path("id");
        JsonNode debt = entry.path("debtValue");
        JsonNode birthday = entry.path("birthdayToday");
        long actualId = id.asLong();
        int actualDebt = debt.asInt();
        boolean birthdayToday = birthday.asBoolean();
        assertThat(actualId).isEqualTo(CUSTOMER_ID);
        assertThat(actualDebt).isEqualTo(CUSTOMER_DEBT);
        assertThat(birthdayToday).isTrue();
        assertThat(entry).hasSize(CUSTOMER_JSON_FIELD_COUNT);
        assertThat(json).hasSize(PAGE_JSON_FIELD_COUNT);
    }

    @Test
    void keepsTransactionJsonAndPagingContract() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        LocalDate date = LocalDate.parse(FORM_DATE);
        transaction.setDate(date);
        transaction.setType(TransactionType.PAYMENT);
        transaction.setDetail("Test payment");
        transaction.setAmount(PAYMENT_AMOUNT);
        transaction.setBalance(CUSTOMER_DEBT);
        List<Transaction> transactions = List.of(transaction);
        Page<Transaction> page = new PageImpl<>(transactions);
        TransactionQueries queryStub = doReturn(page).when(transactionQueries);
        Long matchedId = eq(CUSTOMER_ID);
        Pageable matchedPage = any(Pageable.class);
        queryStub.listByCustomer(matchedId, matchedPage);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/customers/{id}/transactions", CUSTOMER_ID);
        request.accept(MediaType.APPLICATION_JSON);
        MvcResult result = mockMvc.perform(request).andReturn();
        assertStatus(result, HttpStatus.OK);

        JsonNode json = readJson(result);
        JsonNode content = json.path("content");
        JsonNode entry = content.get(0);
        JsonNode type = entry.path("typeKey");
        JsonNode detail = entry.path("detail");
        String typeKey = type.asText();
        String description = detail.asText();
        assertThat(typeKey).isEqualTo("PAYMENT");
        assertThat(description).isEqualTo("Test payment");
        assertThat(entry).hasSize(TRANSACTION_JSON_FIELD_COUNT);
        assertThat(json).hasSize(PAGE_JSON_FIELD_COUNT);
    }

    @Test
    void keepsCustomerDetailScoreAndReversesCyclesFromThePresentationService() throws Exception {
        ScoreResult cycleResult = CustomerScoreCalculator.evaluate(null);
        CycleScore firstCycle = new CycleScore(1, null, null, cycleResult);
        CycleScore secondCycle = new CycleScore(SECOND_CYCLE_NUMBER, null, null, cycleResult);
        List<CycleScore> cycles = List.of(firstCycle, secondCycle);
        String explanation = "Existing score explanation";
        CustomerScorePresentation presentation = new CustomerScorePresentation(CUSTOMER_SCORE, explanation, cycles);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, sort);
        Page<Transaction> transactions = Page.empty(pageable);
        doReturn(customer).when(customerQueries).get(CUSTOMER_ID);
        doReturn(presentation).when(presentationService).getScorePresentation(customer);
        doReturn(transactions).when(transactionQueries).listByCustomer(CUSTOMER_ID, pageable);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/customers/{id}", CUSTOMER_ID);

        MvcResult result = mockMvc.perform(request).andReturn();

        assertStatus(result, HttpStatus.OK);
        ModelAndView modelAndView = result.getModelAndView();
        assertThat(modelAndView).isNotNull();
        String viewName = modelAndView.getViewName();
        Map<String, Object> model = modelAndView.getModel();
        List<CycleScore> expectedCycles = List.of(secondCycle, firstCycle);
        assertThat(viewName).isEqualTo("customers/detail");
        assertThat(model).containsEntry("customerScore", CUSTOMER_SCORE)
                .containsEntry("customerScoreExplanation", explanation)
                .containsEntry("customerScoreCycles", expectedCycles)
                .containsEntry("customer", customer)
                .containsEntry("transactionsPage", transactions);
        assertThat(cycles).containsExactly(firstCycle, secondCycle);
        verify(presentationService).getScorePresentation(customer);
        verifyNoInteractions(customerScoreService);
    }

    @Test
    void keepsRankingRouteAndDirectionModel() throws Exception {
        Pageable pageable = PageRequest.of(0, MAX_RANKING_PAGE_SIZE);
        Page<CustomerRankingEntry> page = Page.empty(pageable);
        doReturn(page).when(customerRankingService).getRanking(pageable, true);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/customers/ranking");
        request.param("direction", "asc");
        MvcResult result = mockMvc.perform(request).andReturn();
        assertStatus(result, HttpStatus.OK);
        ModelAndView modelAndView = result.getModelAndView();
        assertThat(modelAndView).isNotNull();
        String viewName = modelAndView.getViewName();
        Map<String, Object> model = modelAndView.getModel();
        assertThat(viewName).isEqualTo("customers/ranking");
        assertThat(model).containsEntry("rankingPage", page)
                .containsEntry("direction", "asc").containsEntry("nextDirection", "desc");
    }

    @ParameterizedTest
    @CsvSource({
        "ALL, ALL, false",
        "MONTHS, PAYMENT, true",
        "months, payment, true",
        "unexpected, unexpected, false"
    })
    void keepsReportSelectionAndPdfDownload(String range, String type, boolean filtered) throws Exception {
        LocalDate today = LocalDate.now(reportClock);
        LocalDate recentDate = today.withDayOfMonth(1);
        LocalDate oldDate = recentDate.minusMonths(1);
        Transaction recentPayment = new Transaction();
        recentPayment.setDate(recentDate);
        recentPayment.setType(TransactionType.PAYMENT);
        Transaction oldPayment = new Transaction();
        oldPayment.setDate(oldDate);
        oldPayment.setType(TransactionType.PAYMENT);
        Transaction sale = new Transaction();
        sale.setDate(recentDate);
        sale.setType(TransactionType.SALE);
        List<Transaction> transactions = List.of(recentPayment, oldPayment, sale);
        List<Transaction> selected = filtered ? List.of(recentPayment) : transactions;
        String rangeLabel = filtered ? "Últimos 1 mes" : "Todas las transacciones";
        TransactionType filterType = filtered ? TransactionType.PAYMENT : null;
        byte[] pdf = "%PDF-test".getBytes(StandardCharsets.US_ASCII);
        doReturn(customer).when(customerQueries).get(CUSTOMER_ID);
        doReturn(transactions).when(transactionQueries).listAllByCustomer(CUSTOMER_ID);
        doReturn(pdf).when(customerReportService).generateTransactionsReport(customer, selected, rangeLabel, filterType);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(
                "/customers/{id}/reports/transactions", CUSTOMER_ID);
        request.param("range", range);
        request.param("months", "1");
        request.param("type", type);
        request.accept(MediaType.APPLICATION_PDF);
        MvcResult result = mockMvc.perform(request).andReturn();
        assertStatus(result, HttpStatus.OK);
        MockHttpServletResponse response = result.getResponse();
        String contentType = response.getContentType();
        String disposition = response.getHeader(HttpHeaders.CONTENT_DISPOSITION);
        byte[] body = response.getContentAsByteArray();
        assertThat(contentType).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        assertThat(disposition).isEqualTo("attachment; filename=\"casero-informe-Test-Customer.pdf\"");
        assertThat(body).isEqualTo(pdf);
    }

    private void assertStatus(MvcResult result, HttpStatus expectedStatus) {
        MockHttpServletResponse response = result.getResponse();
        int actualStatus = response.getStatus();
        int expectedCode = expectedStatus.value();
        assertThat(actualStatus).isEqualTo(expectedCode);
    }

    private void assertRedirect(MvcResult result, String expectedRedirect) {
        assertStatus(result, HttpStatus.FOUND);
        MockHttpServletResponse response = result.getResponse();
        String redirect = response.getRedirectedUrl();
        assertThat(redirect).isEqualTo(expectedRedirect);
    }

    private Map<String, Object> captureAnonymousAudit() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.captor();
        AuditEventService auditVerification = verify(auditEventService);
        AuditEventType eventType = eq(AuditEventType.ACTION);
        AuditContext anonymousContext = new AuditContext(null, "127.0.0.1", null);
        Map<String, Object> payload = captor.capture();
        AuditContext expectedContext = eq(anonymousContext);
        auditVerification.logEvent(eventType, payload, expectedContext);
        return captor.getValue();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        String content = response.getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(content);
    }
}
