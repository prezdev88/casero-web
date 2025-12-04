package cl.casero.migration.web.controller;

import cl.casero.migration.service.AppConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.domain.AppUser;
import cl.casero.migration.service.AuditEventService;
import cl.casero.migration.web.security.CaseroUserDetails;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/config")
public class AdminConfigController {

    private final AppConfigService appConfigService;
    private final AuditEventService auditEventService;

    @GetMapping
    public String config(Model model) {
        return "redirect:/admin";
    }

    @PostMapping("/update")
    public String updateConfig(
        @RequestParam("configKey") String configKey,
        @RequestParam("value") String value,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        try {
            appConfigService.updateValue(configKey, value);
            redirectAttributes.addFlashAttribute("message", "Configuración actualizada");
            auditEventService.logEvent(
                AuditEventType.ACTION,
                currentUser(authentication),
                actionPayload("APP_CONFIG_UPDATED", Map.of(
                    "key", configKey,
                    "value", value
                )),
                request);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/config";
    }

    private AppUser currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CaseroUserDetails details) {
            return details.getAppUser();
        }
        return null;
    }

    private Map<String, Object> actionPayload(String type, Map<String, Object> data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("data", data != null ? data : Map.of());
        return payload;
    }
}
