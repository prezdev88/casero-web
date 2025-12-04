package cl.casero.migration.web.controller;

import cl.casero.migration.domain.enums.UserRole;
import cl.casero.migration.service.AppConfigService;
import cl.casero.migration.service.AppUserService;
import cl.casero.migration.service.dto.CreateUserForm;
import cl.casero.migration.service.dto.UpdatePinForm;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AppUserService appUserService;
    private final AppConfigService appConfigService;

    @GetMapping("/admin")
    public String admin(Model model, HttpServletRequest request) {
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
        model.addAttribute("configs", appConfigService.listAll());

        return "admin/admin";
    }
}
