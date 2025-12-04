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

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/config")
public class AdminConfigController {

    private final AppConfigService appConfigService;

    @GetMapping
    public String config(Model model) {
        return "redirect:/admin";
    }

    @PostMapping("/update")
    public String updateConfig(
        @RequestParam("configKey") String configKey,
        @RequestParam("value") String value,
        RedirectAttributes redirectAttributes
    ) {
        try {
            appConfigService.updateValue(configKey, value);
            redirectAttributes.addFlashAttribute("message", "Configuración actualizada");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/config";
    }
}
