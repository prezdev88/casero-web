package cl.casero.migration.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import cl.casero.migration.domain.enums.UserRole;
import cl.casero.migration.service.dto.UserIdentity;
import cl.casero.migration.web.security.CaseroUserDetails;

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
        UserIdentity identity = userDetails.getIdentity();
        Long id = identity.id();
        String name = identity.name();
        UserRole role = identity.role();
        boolean admin = identity.isAdmin();
        return new CurrentUser(id, name, role, admin);
    }

    public record CurrentUser(
        Long id, 
        String name, 
        UserRole role, 
        boolean admin
    ) {}
}
