package cl.casero.migration.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
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
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.TransactionType;
import cl.casero.migration.service.TransactionQueries;
import cl.casero.migration.util.TransactionTypePresentation;
import cl.casero.migration.util.TransactionTypeUtil;

@ExtendWith(MockitoExtension.class)
class TransactionPresentationViewsTest {

    private static final int PAGE_SIZE = 10;
    private static final int HTTP_OK = 200;
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-05T12:00:00Z");

    @Mock
    private TransactionQueries queries;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        TransactionController controller = new TransactionController(queries);
        String encoding = StandardCharsets.UTF_8.name();
        ClassLoaderTemplateResolver templates = new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/");
        templates.setSuffix(".html");
        templates.setCharacterEncoding(encoding);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templates);
        ThymeleafViewResolver views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        views.setCharacterEncoding(encoding);
        StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(controller);
        builder.setViewResolvers(views);
        mvc = builder.build();
    }

    @ParameterizedTest
    @CsvSource({
        "SALE, Venta, 🛒", "PAYMENT, Abono, 💰", "REFUND, Devolución, ↩️",
        "DEBT_FORGIVENESS, Condonación de deuda, ❤️", "INITIAL_BALANCE, Saldo inicial, ⚖️",
        "FAULT_DISCOUNT, Descuento por falla, 🛠️"
    })
    void sharesTheExactLabelsAndIconsAcrossJavaScriptJavaAndServerRenderedViews(
        TransactionType type, String label, String icon
    ) throws Exception {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("<Customer>");
        Transaction transaction = new Transaction();
        transaction.setCustomer(customer);
        transaction.setType(type);
        transaction.setCreatedAt(CREATED_AT);
        transaction.setAmount(0);
        transaction.setBalance(0);
        Sort dateSort = Sort.by(Sort.Direction.DESC, "date");
        Sort creationSort = Sort.by(Sort.Direction.DESC, "createdAt");
        Sort sort = dateSort.and(creationSort);
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, sort);
        List<Transaction> content = List.of(transaction);
        Page<Transaction> page = new PageImpl<>(content, pageable, 1);
        doReturn(page).when(queries).listAll(type, pageable);
        String typeName = type.name();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/transactions");
        request.param("type", typeName);

        MvcResult result = mvc.perform(request).andReturn();

        MockHttpServletResponse response = result.getResponse();
        int status = response.getStatus();
        String html = response.getContentAsString();
        assertThat(status).isEqualTo(HTTP_OK);
        assertThat(html).contains(label, icon, "&lt;Customer&gt;", "/customers/1");
        String utilityLabel = TransactionTypeUtil.label(type);
        assertThat(utilityLabel).isEqualTo(label);
        String key = typeName.toLowerCase();
        Map<String, Map<String, String>> metadata = TransactionTypePresentation.byKey();
        Map<String, String> details = metadata.get(key);
        assertThat(details).containsEntry("label", label).containsEntry("icon", icon);
    }
}
