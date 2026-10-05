package cl.casero.migration.web.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.domain.enums.UserRole;
import cl.casero.migration.service.AppConfigService;
import cl.casero.migration.service.AppUserService;
import cl.casero.migration.service.AuditEventService;
import cl.casero.migration.service.dto.AuditContext;
import cl.casero.migration.service.dto.CreateUserForm;
import cl.casero.migration.service.dto.UpdatePinForm;
import cl.casero.migration.web.controller.AdminConfigController;
import cl.casero.migration.web.controller.AdminUserController;
import cl.casero.migration.web.interceptor.AuditViewInterceptor;
import cl.casero.migration.web.security.CaseroUserDetails;
import cl.casero.migration.web.security.LoginFailureHandler;
import cl.casero.migration.web.security.LoginSuccessHandler;

@ExtendWith(MockitoExtension.class)
class AuditEmittersTest {

    private static final long USER_ID = 7L;
    private static final String FORWARDED_IP = "198.51.100.8";
    private static final String USER_AGENT = "Audit test browser";
    private static final String USER_NAME = "Test User";
    private static final String TEST_PIN = "1234";

    @Mock
    private AuditEventService audit;

    @Mock
    private AppUserService users;

    @Mock
    private AppConfigService configuration;

    private final AuditContextFactory factory = new AuditContextFactory();
    private AppUser actor;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        actor = new AppUser();
        actor.setId(USER_ID);
        actor.setName(USER_NAME);
        actor.setRole(UserRole.NORMAL);
        CaseroUserDetails details = new CaseroUserDetails(actor);
        authentication = new TestingAuthenticationToken(details, null);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void keepsLoginActorPayloadMetadataAndRedirect() throws Exception {
        LoginSuccessHandler handler = new LoginSuccessHandler(audit, factory);
        MockHttpServletRequest request = requestWithMetadata();
        request.setRequestURI("/login");
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        String sessionId = session.getId();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        Map<String, Object> payload = Map.of("path", "/login", "sessionId", sessionId);
        assertEvent(AuditEventType.LOG_IN, payload, actor);
        String redirect = response.getRedirectedUrl();
        assertThat(redirect).isEqualTo("/customers");
    }

    @Test
    void keepsLoginFailureAnonymousReasonMetadataAndRedirect() throws Exception {
        LoginFailureHandler handler = new LoginFailureHandler(audit, factory);
        MockHttpServletRequest request = requestWithMetadata();
        request.setRequestURI("/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        BadCredentialsException failure = new BadCredentialsException("Invalid credentials");

        handler.onAuthenticationFailure(request, response, failure);

        Map<String, Object> payload = Map.of("reason", "Invalid credentials", "path", "/login");
        assertEvent(AuditEventType.LOG_ERROR, payload, null);
        String redirect = response.getRedirectedUrl();
        assertThat(redirect).isEqualTo("/login?error");
    }

    @ParameterizedTest
    @CsvSource({"GET, true", "HEAD, false"})
    void keepsSafePageViewsWithAuthenticatedOrAnonymousActors(String method, boolean authenticated) {
        AuditViewInterceptor interceptor = new AuditViewInterceptor(audit, factory);
        MockHttpServletRequest request = requestWithMetadata();
        request.setMethod(method);
        request.setQueryString("q=test");
        if (authenticated) {
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authentication);
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = interceptor.preHandle(request, response, this);

        AppUser expectedUser = authenticated ? actor : null;
        Map<String, Object> payload = Map.of("path", "/customers?q=test");
        assertEvent(AuditEventType.PAGE_VIEW, payload, expectedUser);
        assertThat(allowed).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "POST, /customers, Accept, text/html",
        "GET, /login, Accept, text/html",
        "GET, /customers, Accept, application/json",
        "GET, /customers, X-Requested-With, XMLHttpRequest"
    })
    void keepsRequestsExcludedFromPageViewAuditing(String method, String path, String header, String value) {
        AuditViewInterceptor interceptor = new AuditViewInterceptor(audit, factory);
        MockHttpServletRequest request = requestWithMetadata();
        request.setMethod(method);
        request.setRequestURI(path);
        request.addHeader(header, value);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, this);

        assertThat(allowed).isTrue();
        verifyNoInteractions(audit);
    }

    @Test
    void keepsAdministrativeUserCreationPayloadAndActor() {
        AdminUserController controller = new AdminUserController(users, audit, factory);
        CreateUserForm form = new CreateUserForm();
        form.setName(USER_NAME);
        form.setRole(UserRole.NORMAL);
        form.setPin(TEST_PIN);
        BindingResult validation = new BeanPropertyBindingResult(form, "createUserForm");
        RedirectAttributes attributes = new RedirectAttributesModelMap();
        MockHttpServletRequest request = requestWithMetadata();
        doReturn(actor).when(users).create(USER_NAME, UserRole.NORMAL, TEST_PIN);

        String redirect = controller.createUser(form, validation, attributes, authentication, request);

        Map<String, Object> data = Map.of("id", USER_ID, "name", USER_NAME, "role", UserRole.NORMAL);
        Map<String, Object> payload = Map.of("type", "ADMIN_USER_CREATED", "data", data);
        assertEvent(AuditEventType.ACTION, payload, actor);
        assertThat(redirect).isEqualTo("redirect:/admin/users");
    }

    @Test
    void keepsAdministrativePinUpdatePayloadAndActor() {
        AdminUserController controller = new AdminUserController(users, audit, factory);
        UpdatePinForm form = new UpdatePinForm();
        form.setUserId(USER_ID);
        form.setPin(TEST_PIN);
        BindingResult validation = new BeanPropertyBindingResult(form, "updatePinForm");
        RedirectAttributes attributes = new RedirectAttributesModelMap();
        MockHttpServletRequest request = requestWithMetadata();

        String redirect = controller.updatePin(form, validation, attributes, authentication, request);

        Map<String, Object> data = Map.of("userId", USER_ID);
        Map<String, Object> payload = Map.of("type", "ADMIN_USER_PIN_UPDATED", "data", data);
        assertEvent(AuditEventType.ACTION, payload, actor);
        verify(users).updatePin(USER_ID, TEST_PIN);
        assertThat(redirect).isEqualTo("redirect:/admin/users");
    }

    @Test
    void keepsConfigurationUpdatePayloadAndActor() {
        AdminConfigController controller = new AdminConfigController(configuration, audit, factory);
        RedirectAttributes attributes = new RedirectAttributesModelMap();
        MockHttpServletRequest request = requestWithMetadata();

        String redirect = controller.updateConfig("audit.enabled", "true", attributes, authentication, request);

        Map<String, Object> data = Map.of("key", "audit.enabled", "value", "true");
        Map<String, Object> payload = Map.of("type", "APP_CONFIG_UPDATED", "data", data);
        assertEvent(AuditEventType.ACTION, payload, actor);
        verify(configuration).updateValue("audit.enabled", "true");
        assertThat(redirect).isEqualTo("redirect:/admin/config");
    }

    private MockHttpServletRequest requestWithMetadata() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/customers");
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("X-Forwarded-For", " 198.51.100.8, 192.0.2.20 ");
        request.addHeader("User-Agent", USER_AGENT);
        return request;
    }

    private void assertEvent(AuditEventType type, Map<String, Object> payload, AppUser user) {
        AuditContext expected = new AuditContext(user, FORWARDED_IP, USER_AGENT);
        verify(audit).logEvent(type, payload, expected);
    }
}
