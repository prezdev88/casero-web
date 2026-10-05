package cl.casero.migration.web.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.service.AuditEventService;
import cl.casero.migration.service.dto.AuditContext;
import cl.casero.migration.service.dto.UserIdentity;
import cl.casero.migration.web.audit.AuditContextFactory;

@RequiredArgsConstructor
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AuditEventService auditEventService;
    private final AuditContextFactory auditContextFactory;

    {
        setDefaultTargetUrl("/customers");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {
        UserIdentity user = extractUser(authentication);
        Map<String, Object> payload = buildPayload(request);
        AuditContext context = auditContextFactory.from(user, request);
        auditEventService.logEvent(AuditEventType.LOG_IN, payload, context);
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private UserIdentity extractUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CaseroUserDetails details) {
            return details.getIdentity();
        }
        return null;
    }

    private Map<String, Object> buildPayload(HttpServletRequest request) {
        Map<String, Object> payload = new HashMap<>();
        if (request != null) {
            payload.put("path", request.getRequestURI());
            if (request.getSession(false) != null) {
                payload.put("sessionId", request.getSession(false).getId());
            }
        }
        return payload;
    }
}
