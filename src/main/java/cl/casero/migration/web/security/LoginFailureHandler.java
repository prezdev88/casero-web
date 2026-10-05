package cl.casero.migration.web.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.service.AuditEventService;
import cl.casero.migration.service.dto.AuditContext;
import cl.casero.migration.web.audit.AuditContextFactory;

@RequiredArgsConstructor
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AuditEventService auditEventService;
    private final AuditContextFactory auditContextFactory;

    {
        setDefaultFailureUrl("/login?error");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        String reason = exception != null && exception.getMessage() != null ? exception.getMessage() : "unknown";
        Map<String, Object> payload = new HashMap<>();
        payload.put("reason", reason);
        if (request != null) {
            payload.put("path", request.getRequestURI());
        }
        AuditContext context = auditContextFactory.from(null, request);
        auditEventService.logEvent(AuditEventType.LOG_ERROR, payload, context);
        super.onAuthenticationFailure(request, response, exception);
    }
}
