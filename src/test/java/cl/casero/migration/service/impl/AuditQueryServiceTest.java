package cl.casero.migration.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import cl.casero.migration.domain.AuditEvent;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.repository.AuditEventRepository;
import cl.casero.migration.service.AuditQueries;
import cl.casero.migration.web.controller.AdminAuditController;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {

    private static final int HTTP_OK = 200;

    @Mock
    private AuditEventRepository repository;

    private AnnotationConfigApplicationContext context;
    private RecordingTransactionManager manager;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        manager = new RecordingTransactionManager();
        context = new AnnotationConfigApplicationContext();
        context.register(TransactionTestConfiguration.class);
        context.registerBean(AuditEventRepository.class, () -> repository);
        context.registerBean(PlatformTransactionManager.class, () -> manager);
        context.registerBean(AuditQueryService.class);
        context.refresh();
        AuditQueries queries = context.getBean(AuditQueries.class);
        AdminAuditController controller = new AdminAuditController(queries);
        InternalResourceViewResolver views = new InternalResourceViewResolver("/test-views/", ".html");
        StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(controller);
        builder.setViewResolvers(views);
        mvc = builder.build();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @ParameterizedTest
    @CsvSource(value = {
        "NULL,NULL,-3,0,NULL,NULL,0,1",
        "ACTION,SALE,2,1000,ACTION,SALE,2,100",
        "LOG_IN,NULL,0,20,LOG_IN,NULL,0,20",
        "unknown,' PAYMENT ',0,20,NULL,PAYMENT,0,20",
        "' ACTION ',' ',0,20,NULL,NULL,0,20"
    }, nullValues = "NULL")
    void preservesAllFilterBranchesHttpNormalizationPagingAndReadOnlyTransactions(
        String rawEventType, String rawPayloadType, int page, int size,
        AuditEventType eventType, String payloadType, int expectedPage, int expectedSize
    ) throws Exception {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(expectedPage, expectedSize, sort);
        Page<AuditEvent> events = Page.empty(pageable);
        Answer<Page<AuditEvent>> answer = invocation -> {
            boolean active = TransactionSynchronizationManager.isActualTransactionActive();
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            assertThat(active).isTrue();
            assertThat(readOnly).isTrue();
            return events;
        };
        AuditEventRepository stub = doAnswer(answer).when(repository);
        if (eventType != null && payloadType != null) {
            stub.findByEventTypeAndPayloadType(eventType, payloadType, pageable);
        } else if (payloadType != null) {
            stub.findByPayloadType(payloadType, pageable);
        } else if (eventType != null) {
            stub.findByEventTypeOrderByCreatedAtDesc(eventType, pageable);
        } else {
            stub.findAllByOrderByCreatedAtDesc(pageable);
        }

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/admin/audit");
        String rawPage = Integer.toString(page);
        String rawSize = Integer.toString(size);
        request.param("page", rawPage).param("size", rawSize);
        if (rawEventType != null) {
            request.param("eventType", rawEventType);
        }

        if (rawPayloadType != null) {
            request.param("payloadType", rawPayloadType);
        }

        MvcResult result = mvc.perform(request).andReturn();

        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(HTTP_OK);
        ModelAndView view = result.getModelAndView();
        Map<String, Object> model = view.getModel();
        assertThat(model).containsEntry("events", events).containsEntry("page", expectedPage)
                .containsEntry("size", expectedSize).containsEntry("selectedEventType", eventType)
                .containsEntry("payloadType", payloadType);
        int commits = manager.getCommitCount();
        assertThat(commits).isEqualTo(1);
    }
}
