package cl.casero.migration.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.AuditEvent;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.domain.enums.UserRole;
import cl.casero.migration.repository.AuditEventRepository;
import cl.casero.migration.service.AuditPolicy;
import cl.casero.migration.service.dto.AuditContext;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceImplTest {

    private static final String IP = "198.51.100.8";
    private static final String USER_AGENT = "Audit test browser";

    @Mock
    private AuditEventRepository repository;

    @Mock
    private AuditPolicy configuration;

    private AuditEventServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuditEventServiceImpl(repository, configuration);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void recordsPreparedMetadataAndCopiesPayloadForAuthenticatedOrAnonymousUsers(boolean authenticated) {
        enableAudit();
        AppUser actor = authenticated ? new AppUser() : null;
        AuditContext context = new AuditContext(actor, IP, USER_AGENT);
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "TEST_ACTION");
        payload.put("optional", null);

        service.logEvent(AuditEventType.ACTION, payload, context);

        AuditEvent event = savedEvent();
        AuditEventType eventType = event.getEventType();
        AppUser user = event.getUser();
        String ip = event.getIp();
        String userAgent = event.getUserAgent();
        Map<String, Object> savedPayload = event.getPayload();
        assertThat(eventType).isEqualTo(AuditEventType.ACTION);
        assertThat(user).isSameAs(actor);
        assertThat(ip).isEqualTo(IP);
        assertThat(userAgent).isEqualTo(USER_AGENT);
        assertThat(savedPayload).isEqualTo(payload).isNotSameAs(payload);
        payload.put("type", "CHANGED_ACTION");
        assertThat(savedPayload).containsEntry("type", "TEST_ACTION").containsEntry("optional", null);
    }

    @Test
    void recordsAnEmptyPayloadWithoutSourceMetadata() {
        enableAudit();
        AuditContext context = new AuditContext(null, null, null);

        service.logEvent(AuditEventType.LOG_ERROR, null, context);

        AuditEvent event = savedEvent();
        Map<String, Object> payload = event.getPayload();
        String ip = event.getIp();
        String userAgent = event.getUserAgent();
        assertThat(payload).isEmpty();
        assertThat(ip).isNull();
        assertThat(userAgent).isNull();
    }

    @Test
    void skipsPersistenceWhenAuditIsDisabled() {
        AuditContext context = new AuditContext(null, IP, USER_AGENT);
        Map<String, Object> payload = Map.of();

        service.logEvent(AuditEventType.ACTION, payload, context);

        verifyNoInteractions(repository);
    }

    @Test
    void skipsMissingEventTypes() {
        enableAudit();
        AuditContext context = new AuditContext(null, IP, USER_AGENT);
        Map<String, Object> payload = Map.of();

        service.logEvent(null, payload, context);

        verifyNoInteractions(repository);
    }

    @Test
    void skipsAdministratorEvents() {
        enableAudit();
        AppUser actor = new AppUser();
        actor.setRole(UserRole.ADMIN);
        AuditContext context = new AuditContext(actor, IP, USER_AGENT);
        Map<String, Object> payload = Map.of();

        service.logEvent(AuditEventType.ACTION, payload, context);

        verifyNoInteractions(repository);
    }

    @Test
    void doesNotPropagatePersistenceFailures() {
        enableAudit();
        IllegalStateException failure = new IllegalStateException("Audit persistence failed");
        AuditEventRepository stub = doThrow(failure).when(repository);
        AuditEvent matchedEvent = any(AuditEvent.class);
        stub.save(matchedEvent);
        AuditContext context = new AuditContext(null, IP, USER_AGENT);
        Map<String, Object> payload = Map.of();

        assertThatCode(() -> service.logEvent(AuditEventType.LOG_IN, payload, context)).doesNotThrowAnyException();
    }

    private void enableAudit() {
        doReturn(true).when(configuration).isAuditEnabled();
    }

    private AuditEvent savedEvent() {
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        AuditEventRepository verification = verify(repository);
        AuditEvent event = captor.capture();
        verification.save(event);
        return captor.getValue();
    }
}
