package cl.casero.migration.web.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.service.dto.AuditContext;

class AuditContextFactoryTest {

    private static final String REMOTE_IP = "192.0.2.10";
    private static final String USER_AGENT = "Audit test browser";

    private final AuditContextFactory factory = new AuditContextFactory();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void fallsBackToRemoteAddressWhenForwardedHeaderHasNoText(String forwardedFor) {
        MockHttpServletRequest request = requestWithMetadata();
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }

        AuditContext context = factory.from(null, request);

        String ip = context.ip();
        String userAgent = context.userAgent();
        AppUser user = context.user();
        assertThat(ip).isEqualTo(REMOTE_IP);
        assertThat(userAgent).isEqualTo(USER_AGENT);
        assertThat(user).isNull();
    }

    @ParameterizedTest
    @CsvSource(value = {" 198.51.100.8 |198.51.100.8", " 198.51.100.8 , 192.0.2.20 |198.51.100.8"}, delimiter = '|')
    void keepsTheFirstForwardedAddress(String forwardedFor, String expectedIp) {
        MockHttpServletRequest request = requestWithMetadata();
        request.addHeader("X-Forwarded-For", forwardedFor);
        AppUser actor = new AppUser();

        AuditContext context = factory.from(actor, request);

        String ip = context.ip();
        AppUser user = context.user();
        assertThat(ip).isEqualTo(expectedIp);
        assertThat(user).isSameAs(actor);
    }

    @Test
    void preservesTheActorWithoutRequestMetadata() {
        AppUser actor = new AppUser();

        AuditContext context = factory.from(actor, null);

        AuditContext expected = new AuditContext(actor, null, null);
        assertThat(context).isEqualTo(expected);
    }

    @Test
    void preservesMissingUserAgent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(REMOTE_IP);

        AuditContext context = factory.from(null, request);

        AuditContext expected = new AuditContext(null, REMOTE_IP, null);
        assertThat(context).isEqualTo(expected);
    }

    private MockHttpServletRequest requestWithMetadata() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(REMOTE_IP);
        request.addHeader("User-Agent", USER_AGENT);
        return request;
    }
}
