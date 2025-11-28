package cl.casero.migration.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(
        @RequestParam(value = "error", required = false) Optional<String> error,
        @RequestParam(value = "logout", required = false) Optional<String> logout,
        @RequestParam(value = "timeout", required = false) Optional<String> timeout,
        Model model,
        Authentication authentication
    ) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }

        model.addAttribute("showError", error.isPresent());
        model.addAttribute("showLogout", logout.isPresent());
        model.addAttribute("showTimeout", timeout.isPresent());
        
        return "auth/login";
    }
}
