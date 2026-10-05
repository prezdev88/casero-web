package cl.casero.migration.web.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import cl.casero.migration.service.dto.AuditContext;
import cl.casero.migration.service.dto.UserIdentity;

@Component
public class AuditContextFactory {

    public AuditContext from(UserIdentity user, HttpServletRequest request) {
        if (request == null) {
            return new AuditContext(user, null, null);
        }

        String ip = resolveIp(request);
        String userAgent = request.getHeader("User-Agent");
        return new AuditContext(user, ip, userAgent);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (StringUtils.hasText(forwardedFor)) {
            String[] addresses = forwardedFor.split(",");
            return addresses[0].trim();
        }

        return request.getRemoteAddr();
    }
}
