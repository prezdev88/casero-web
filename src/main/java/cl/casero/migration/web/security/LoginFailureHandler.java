package cl.casero.migration.web.security;

import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.service.AuditEventService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
        auditEventService.logEvent(AuditEventType.LOGIN_FAILED, null, "LOGIN_FAILED reason=" + reason, request);
        super.onAuthenticationFailure(request, response, exception);
    }
}
