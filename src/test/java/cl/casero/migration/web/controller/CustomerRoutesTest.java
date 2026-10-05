package cl.casero.migration.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
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
import cl.casero.migration.service.CustomerReportService;
import cl.casero.migration.service.CustomerScoreService;
import cl.casero.migration.service.CustomerService;
import cl.casero.migration.service.SectorService;
import cl.casero.migration.service.StatisticsService;
import cl.casero.migration.service.TransactionService;
import cl.casero.migration.service.dto.CreateCustomerForm;
import cl.casero.migration.web.audit.CustomerAuditLogger;
import cl.casero.migration.web.security.CaseroUserDetails;

@ExtendWith(MockitoExtension.class)
class CustomerRoutesTest {

    private static final long CUSTOMER_ID = 7L;
    private static final long SECTOR_ID = 3L;
    private static final long TRANSACTION_ID = 11L;
    private static final int PAGE_SIZE = 10;
    private static final int CUSTOMER_JSON_FIELD_COUNT = 9;
    private static final int TRANSACTION_JSON_FIELD_COUNT = 6;
    private static final int PAGE_JSON_FIELD_COUNT = 6;
    private static final int UPDATED_PROFILE_EVENT_COUNT = 2;
    private static final int MAX_RANKING_PAGE_SIZE = 100;
    private static final int CUSTOMER_DEBT = 200;
    private static final int PAYMENT_AMOUNT = 100;
    private static final double CUSTOMER_SCORE = 4.0;
    private static final String FORM_DATE = "2026-10-04";
    private static final ZoneId CUSTOMER_ZONE = ZoneId.of("America/Santiago");

    @Mock
    private CustomerService customerService;

    @Mock
    private CustomerScoreService customerScoreService;

    @Mock
    private TransactionService transactionService;

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

        CustomerAuditLogger auditLogger = new CustomerAuditLogger(auditEventService);
        CustomerController customerViews = new CustomerController(customerService, customerScoreService, transactionService);
        CustomerManagementController management = new CustomerManagementController(
                customerService, sectorService, statisticsService, auditLogger);
        CustomerTransactionController transactions = new CustomerTransactionController(
                customerService, transactionService, auditLogger);
        CustomerRankingController ranking = new CustomerRankingController(customerScoreService);
        CustomerReportController reports = new CustomerReportController(
                customerService, transactionService, customerReportService);
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
        doReturn(customer).when(customerService).get(CUSTOMER_ID);
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
        verifyNoInteractions(customerService, transactionService, auditEventService);
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

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.captor();
        VerificationMode expectedEvents = times(UPDATED_PROFILE_EVENT_COUNT);
        AuditEventService auditVerification = verify(auditEventService, expectedEvents);
        AuditEventType eventType = eq(AuditEventType.ACTION);
        AppUser anonymous = isNull();
        Map<String, Object> capturedPayload = captor.capture();
        HttpServletRequest capturedRequest = any(HttpServletRequest.class);
        auditVerification.logEvent(eventType, anonymous, capturedPayload, capturedRequest);
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
        CustomerService creationStub = doReturn(customer).when(customerService);
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

        Map<String, Object> data = Map.of("customerId", CUSTOMER_ID, "name", "Test Customer",
                "sectorId", SECTOR_ID, "address", "Test Address");
        Map<String, Object> payload = Map.of("type", "CREATE_CUSTOMER", "data", data);
        AuditEventService auditVerification = verify(auditEventService);
        AuditEventType eventType = eq(AuditEventType.ACTION);
        AppUser expectedActor = eq(actor);
        Map<String, Object> expectedPayload = eq(payload);
        HttpServletRequest actualRequest = any(HttpServletRequest.class);
        auditVerification.logEvent(eventType, expectedActor, expectedPayload, actualRequest);
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

        verify(transactionService).delete(TRANSACTION_ID);
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
        doReturn(page).when(customerService).search("Test", pageable);
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
        TransactionService queryStub = doReturn(page).when(transactionService);
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
    void keepsRankingRouteAndDirectionModel() throws Exception {
        Pageable pageable = PageRequest.of(0, MAX_RANKING_PAGE_SIZE);
        Page<CustomerScoreService.RankingEntry> page = Page.empty(pageable);
        doReturn(page).when(customerScoreService).getRanking(pageable, true);
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
    @CsvSource({"ALL, ALL, false", "MONTHS, PAYMENT, true"})
    void keepsReportSelectionAndPdfDownload(String range, String type, boolean filtered) throws Exception {
        LocalDate today = LocalDate.now();
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
        doReturn(customer).when(customerService).get(CUSTOMER_ID);
        doReturn(transactions).when(transactionService).listAllByCustomer(CUSTOMER_ID);
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
        AppUser anonymous = isNull();
        Map<String, Object> payload = captor.capture();
        HttpServletRequest request = any(HttpServletRequest.class);
        auditVerification.logEvent(eventType, anonymous, payload, request);
        return captor.getValue();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        String content = response.getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(content);
    }
}
