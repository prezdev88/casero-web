package cl.casero.migration.web.interceptor;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.service.AuditEventService;
import cl.casero.migration.web.security.CaseroUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuditViewInterceptor implements HandlerInterceptor {

    private final AuditEventService auditEventService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isJsonRequest(request)) {
            return true;
        }
        if (!isSafeMethod(request)) {
            return true;
        }
        auditEventService.logEvent(AuditEventType.PAGE_VIEW, resolveUser(), buildPayload(request), request);
        return true;
    }

    private AppUser resolveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CaseroUserDetails details) {
            return details.getAppUser();
        }
        return null;
    }

    private String buildPayload(HttpServletRequest request) {
        if (request == null) {
            return "PAGE_VIEW";
        }
        String key = request.getRequestURI();
        String label = mapPathToTitle(key);
        return label != null ? label : key;
    }

    private String mapPathToTitle(String path) {
        if (path == null) {
            return null;
        }
        // Exact matches
        switch (path) {
            case "/", "/customers":
                return "LIST_CUSTOMERS";
            case "/customers/new":
                return "CREATE_CUSTOMER";
            case "/customers/ranking":
                return "CUSTOMER_RANKING";
            case "/statistics":
                return "TOTAL_DEBT";
            case "/statistics/monthly":
                return "MONTHLY_STATS";
            case "/transactions":
                return "LIST_TRANSACTIONS";
            case "/debtors":
                return "LIST_DEBTORS";
            case "/admin":
                return "ADMIN_HOME";
            case "/admin/users":
                return "LIST_USERS";
            case "/admin/config":
                return "APP_CONFIG";
            case "/admin/audit":
                return "AUDIT_LOGS";
            case "/login":
                return "LOGIN_PAGE";
            default:
                break;
        }

        // Pattern matches
        if (path.matches("/customers/\\d+$")) {
            return "VIEW_CUSTOMER";
        }

        if (path.matches("/customers/\\d+/reports/transactions.*")) {
            return "CUSTOMER_PDF_REPORT";
        }

        if (path.matches("/customers/\\d+/actions/.*")) {
            return mapCustomerAction(path);
        }

        return null;
    }

    private String mapCustomerAction(String path) {
        if (path.contains("/actions/payment")) {
            return "CUSTOMER_PAYMENT";
        }
        if (path.contains("/actions/sale")) {
            return "CUSTOMER_SALE";
        }
        if (path.contains("/actions/refund")) {
            return "CUSTOMER_REFUND";
        }
        if (path.contains("/actions/fault-discount")) {
            return "CUSTOMER_FAULT_DISCOUNT";
        }
        if (path.contains("/actions/forgiveness")) {
            return "CUSTOMER_FORGIVENESS";
        }
        if (path.contains("/actions/address/edit")) {
            return "CUSTOMER_ADDRESS_EDIT";
        }
        if (path.contains("/actions/address")) {
            return "CUSTOMER_ADDRESS_VIEW";
        }
        if (path.contains("/actions/name/edit")) {
            return "CUSTOMER_NAME_EDIT";
        }
        if (path.contains("/actions/sector/edit")) {
            return "CUSTOMER_SECTOR_EDIT";
        }
        return "CUSTOMER_ACTION";
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
}
