package cl.casero.migration.web.security;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.service.AuditEventService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

@RequiredArgsConstructor
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AuditEventService auditEventService;

    {
        setDefaultTargetUrl("/customers");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {
        auditEventService.logEvent(AuditEventType.LOGIN_SUCCESS, extractUser(authentication), buildPayload(request), request);
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private AppUser extractUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CaseroUserDetails details) {
            return details.getAppUser();
        }
        return null;
    }

    private String buildPayload(HttpServletRequest request) {
        if (request == null) {
            return "LOGIN_SUCCESS";
        }
        String sessionId = request.getSession(false) != null ? request.getSession(false).getId() : null;
        return sessionId != null ? "LOGIN_SUCCESS sessionId=" + sessionId : "LOGIN_SUCCESS";
    }
}
