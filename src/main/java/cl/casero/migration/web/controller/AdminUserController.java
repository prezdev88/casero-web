package cl.casero.migration.web.controller;

import cl.casero.migration.domain.enums.UserRole;
import cl.casero.migration.service.AppUserService;
import cl.casero.migration.service.dto.CreateUserForm;
import cl.casero.migration.service.dto.UpdatePinForm;
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

@Controller
@AllArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AppUserService appUserService;

    @GetMapping
    public String users(Model model) {
        if (!model.containsAttribute("createUserForm")) {
            model.addAttribute("createUserForm", new CreateUserForm());
        }

        if (!model.containsAttribute("updatePinForm")) {
            model.addAttribute("updatePinForm", new UpdatePinForm());
        }

        if (!model.containsAttribute("pinErrorUserId")) {
            model.addAttribute("pinErrorUserId", null);
        }

        if (!model.containsAttribute("pinError")) {
            model.addAttribute("pinError", null);
        }

        model.addAttribute("users", appUserService.listAll());
        model.addAttribute("roles", Arrays.asList(UserRole.values()));

        return "admin/users";
    }

    @PostMapping("/create")
    public String createUser(
        @Valid @ModelAttribute("createUserForm") CreateUserForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            preserveForm("createUserForm", form, result, redirectAttributes);
            return "redirect:/admin/users";
        }

        try {
            appUserService.create(form.getName(), form.getRole(), form.getPin());
            redirectAttributes.addFlashAttribute("message", "Usuario creado correctamente");
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
        RedirectAttributes redirectAttributes
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
}
