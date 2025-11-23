package cl.casero.migration.web;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.UserRole;
import cl.casero.migration.web.security.CaseroUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class UserSessionAdvice {

    @ModelAttribute("currentUser")
    public CurrentUser currentUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CaseroUserDetails userDetails)) {
            return null;
        }
        AppUser user = userDetails.getUser();
        return new CurrentUser(user.getId(), user.getName(), user.getRole(), userDetails.isAdmin());
    }

    public record CurrentUser(Long id, String name, UserRole role, boolean admin) {
    }
}
