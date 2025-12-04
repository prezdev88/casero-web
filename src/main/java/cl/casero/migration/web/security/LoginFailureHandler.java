package cl.casero.migration.web.security;

import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.service.AuditEventService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

@RequiredArgsConstructor
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AuditEventService auditEventService;

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
        auditEventService.logEvent(AuditEventType.LOG_ERROR, null, payload, request);
        super.onAuthenticationFailure(request, response, exception);
    }
}
