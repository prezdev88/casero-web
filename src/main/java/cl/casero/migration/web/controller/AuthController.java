package cl.casero.migration.web.controller;

import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.service.AuditEventService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuditEventService auditEventService;

    @GetMapping("/login")
    public String login(
        @RequestParam(value = "error", required = false) Optional<String> error,
        @RequestParam(value = "logout", required = false) Optional<String> logout,
        @RequestParam(value = "timeout", required = false) Optional<String> timeout,
        Model model,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }

        auditEventService.logEvent(
            AuditEventType.LOGIN_PAGE_VIEW,
            null,
            "LOGIN_PAGE",
            request);

        model.addAttribute("showError", error.isPresent());
        model.addAttribute("showLogout", logout.isPresent());
        model.addAttribute("showTimeout", timeout.isPresent());
        
        return "auth/login";
    }
}
