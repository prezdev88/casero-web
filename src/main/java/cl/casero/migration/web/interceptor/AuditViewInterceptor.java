package cl.casero.migration.web.interceptor;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.service.AuditEventService;
import cl.casero.migration.service.dto.AuditContext;
import cl.casero.migration.web.audit.AuditContextFactory;
import cl.casero.migration.web.security.CaseroUserDetails;

@Component
@RequiredArgsConstructor
public class AuditViewInterceptor implements HandlerInterceptor {

    private final AuditEventService auditEventService;
    private final AuditContextFactory auditContextFactory;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isJsonRequest(request)) {
            return true;
        }
        if (!isSafeMethod(request)) {
            return true;
        }
        if (isLoginPage(request)) {
            return true;
        }
        AppUser user = resolveUser();
        Map<String, Object> payload = buildPayload(request);
        AuditContext context = auditContextFactory.from(user, request);
        auditEventService.logEvent(AuditEventType.PAGE_VIEW, payload, context);
        return true;
    }

    private AppUser resolveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CaseroUserDetails details) {
            return details.getAppUser();
        }
        return null;
    }

    private Map<String, Object> buildPayload(HttpServletRequest request) {
        Map<String, Object> payload = new HashMap<>();
        if (request == null) {
            return payload;
        }
        String path = request.getRequestURI();
        String query = request.getQueryString();
        payload.put("path", query != null && !query.isBlank() ? path + "?" + query : path);
        return payload;
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String accept = request.getHeader("Accept");
        if (accept != null && accept.toLowerCase().contains("application/json")) {
            return true;
        }
        String xRequestedWith = request.getHeader("X-Requested-With");
        return xRequestedWith != null && xRequestedWith.equalsIgnoreCase("XMLHttpRequest");
    }

    private boolean isSafeMethod(HttpServletRequest request) {
        if (request == null) {
            return true;
        }
        String method = request.getMethod();
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
    }

    private boolean isLoginPage(HttpServletRequest request) {
        return request != null && "/login".equals(request.getRequestURI());
    }
}
