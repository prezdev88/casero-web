package cl.casero.migration.web.controller;

import cl.casero.migration.web.security.CaseroUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CaseroUserDetails) {
            return "redirect:/customers";
        }
        
        return "redirect:/login";
    }
}
