package cl.casero.migration.web.controller;

import java.util.Arrays;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.domain.enums.UserRole;
import cl.casero.migration.service.AppUserService;
import cl.casero.migration.service.AuditEventService;
import cl.casero.migration.service.audit.AuditAction;
import cl.casero.migration.service.dto.AuditContext;
import cl.casero.migration.service.dto.CreateUserForm;
import cl.casero.migration.service.dto.UpdatePinForm;
import cl.casero.migration.web.audit.AuditContextFactory;
import cl.casero.migration.web.security.CaseroUserDetails;

@Controller
@AllArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AppUserService appUserService;
    private final AuditEventService auditEventService;
    private final AuditContextFactory auditContextFactory;

    @GetMapping
    public String users(Model model) {
        return "redirect:/admin";
    }

    @PostMapping("/create")
    public String createUser(
        @Valid @ModelAttribute("createUserForm") CreateUserForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            preserveForm("createUserForm", form, result, redirectAttributes);
            return "redirect:/admin/users";
        }

        try {
            AppUser created = appUserService.create(form.getName(), form.getRole(), form.getPin());
            redirectAttributes.addFlashAttribute("message", "Usuario creado correctamente");
            AppUser actor = currentUser(authentication);
            Long createdId = created.getId();
            String createdName = created.getName();
            UserRole createdRole = created.getRole();
            Map<String, Object> data = Map.of("id", createdId, "name", createdName, "role", createdRole);
            Map<String, Object> payload = AuditAction.ADMIN_USER_CREATED.payload(data);
            AuditContext context = auditContextFactory.from(actor, request);
            auditEventService.logEvent(AuditEventType.ACTION, payload, context);
        } catch (IllegalArgumentException ex) {
            result.reject("createUserForm", ex.getMessage());
            preserveForm("createUserForm", form, result, redirectAttributes);
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/pin")
    public String updatePin(
        @Valid @ModelAttribute("updatePinForm") UpdatePinForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("pinErrorUserId", form.getUserId());
            redirectAttributes.addFlashAttribute("pinError",
                    firstErrorMessage(result));
            return "redirect:/admin/users";
        }

        try {
            appUserService.updatePin(form.getUserId(), form.getPin());
            redirectAttributes.addFlashAttribute("message", "PIN actualizado");
            AppUser actor = currentUser(authentication);
            Long userId = form.getUserId();
            Map<String, Object> data = Map.of("userId", userId);
            Map<String, Object> payload = AuditAction.ADMIN_USER_PIN_UPDATED.payload(data);
            AuditContext context = auditContextFactory.from(actor, request);
            auditEventService.logEvent(AuditEventType.ACTION, payload, context);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("pinErrorUserId", form.getUserId());
            redirectAttributes.addFlashAttribute("pinError", ex.getMessage());
        }

        return "redirect:/admin/users";
    }

    private void preserveForm(
        String attributeName,
        Object attributeValue,
        BindingResult result,
        RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addFlashAttribute(attributeName, attributeValue);
        redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult." + attributeName, result);
    }

    private String firstErrorMessage(BindingResult result) {
        return result.getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse("Error al procesar la solicitud");
    }

    private AppUser currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CaseroUserDetails details) {
            return details.getAppUser();
        }
        return null;
    }
}
