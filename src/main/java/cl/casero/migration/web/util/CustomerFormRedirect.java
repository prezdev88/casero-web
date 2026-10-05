package cl.casero.migration.web.util;

import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public final class CustomerFormRedirect {

    private CustomerFormRedirect() {}

    public static String redirectToAction(
        Long id,
        RedirectAttributes redirectAttributes,
        String attributeName,
        Object form,
        BindingResult result,
        String actionPath
    ) {
        redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult." + attributeName, result);
        redirectAttributes.addFlashAttribute(attributeName, form);

        return "redirect:/customers/" + id + "/actions/" + actionPath;
    }
}
