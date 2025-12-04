package cl.casero.migration.web.controller;

import cl.casero.migration.domain.enums.UserRole;
import cl.casero.migration.service.AppUserService;
import cl.casero.migration.service.dto.CreateUserForm;
import cl.casero.migration.service.dto.UpdatePinForm;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.domain.AppUser;
import cl.casero.migration.service.AuditEventService;
import cl.casero.migration.web.security.CaseroUserDetails;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@AllArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AppUserService appUserService;
    private final AuditEventService auditEventService;

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
            auditEventService.logEvent(
                AuditEventType.ACTION,
                currentUser(authentication),
                "ADMIN_USER_CREATED id=" + created.getId() + " name=" + created.getName() + " role=" + created.getRole(),
                request);
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
            auditEventService.logEvent(
                AuditEventType.ACTION,
                currentUser(authentication),
                "ADMIN_USER_PIN_UPDATED userId=" + form.getUserId(),
                request);
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
