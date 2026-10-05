package cl.casero.migration.web.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Sector;
import cl.casero.migration.service.CustomerCommands;
import cl.casero.migration.service.CustomerQueries;
import cl.casero.migration.service.SectorService;
import cl.casero.migration.service.StatisticsService;
import cl.casero.migration.service.dto.CreateCustomerForm;
import cl.casero.migration.service.dto.UpdateAddressForm;
import cl.casero.migration.service.dto.UpdateBirthdateForm;
import cl.casero.migration.service.dto.UpdateNameForm;
import cl.casero.migration.service.dto.UpdateSectorForm;
import cl.casero.migration.web.audit.CustomerAuditLogger;
import cl.casero.migration.web.util.CustomerFormRedirect;

@Controller
@RequiredArgsConstructor
@RequestMapping("/customers")
public class CustomerManagementController {

    private final CustomerQueries customerQueries;
    private final CustomerCommands customerCommands;
    private final SectorService sectorService;
    private final StatisticsService statisticsService;
    private final CustomerAuditLogger customerAuditLogger;

    @GetMapping("/new")
    public String newCustomer(Model model) {
        if (!model.containsAttribute("customerForm")) {
            CreateCustomerForm form = new CreateCustomerForm();
            model.addAttribute("customerForm", form);
        }

        List<Sector> sectors = sectorService.listAll();
        model.addAttribute("sectors", sectors);
        long customersCount = statisticsService.getCustomersCount();
        model.addAttribute("customersCount", customersCount);

        return "customers/new";
    }

    @PostMapping
    public String createCustomer(
        @Valid @ModelAttribute("customerForm") CreateCustomerForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerForm", result);
            redirectAttributes.addFlashAttribute("customerForm", form);

            return "redirect:/customers/new";
        }

        Customer created = customerCommands.create(form);
        redirectAttributes.addFlashAttribute("message", "Cliente creado correctamente");
        customerAuditLogger.logCustomerCreated(created, authentication, request);

        return "redirect:/customers";
    }

    @PostMapping("/{id}/address")
    public String updateAddress(
        @PathVariable Long id,
        @Valid @ModelAttribute("updateAddressForm") UpdateAddressForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            return CustomerFormRedirect.redirectToAction(id, redirectAttributes, "updateAddressForm", form, result, "address/edit");
        }

        String address = form.getNewAddress();
        customerCommands.updateAddress(id, address);
        redirectAttributes.addFlashAttribute("successMessage", "Dirección actualizada");
        customerAuditLogger.logAddressUpdate(id, form, authentication, request);

        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/sector")
    public String updateSector(
        @PathVariable Long id,
        @Valid @ModelAttribute("updateSectorForm") UpdateSectorForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            return CustomerFormRedirect.redirectToAction(id, redirectAttributes, "updateSectorForm", form, result, "sector/edit");
        }

        Long sectorId = form.getSectorId();
        customerCommands.updateSector(id, sectorId);
        redirectAttributes.addFlashAttribute("successMessage", "Sector actualizado");
        customerAuditLogger.logSectorUpdate(id, form, authentication, request);

        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteCustomer(
        @PathVariable Long id,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        customerCommands.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Cliente eliminado");
        customerAuditLogger.logCustomerDeleted(id, authentication, request);

        return "redirect:/customers";
    }

    @GetMapping("/{id}/actions/address")
    public String viewAddress(@PathVariable Long id, Model model) {
        Customer customer = customerQueries.get(id);
        model.addAttribute("customer", customer);
        return "customers/actions/address";
    }

    @GetMapping("/{id}/actions/address/edit")
    public String editAddress(@PathVariable Long id, Model model) {
        Customer customer = customerQueries.get(id);
        model.addAttribute("customer", customer);

        if (!model.containsAttribute("updateAddressForm")) {
            String address = customer.getAddress();
            UpdateAddressForm form = new UpdateAddressForm(address);
            model.addAttribute("updateAddressForm", form);
        }

        return "customers/actions/address-edit";
    }

    @PostMapping("/{id}/name")
    public String updateName(
        @PathVariable Long id,
        @Valid @ModelAttribute("updateNameForm") UpdateNameForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            return CustomerFormRedirect.redirectToAction(id, redirectAttributes, "updateNameForm", form, result, "name/edit");
        }

        String name = form.getNewName();
        customerCommands.updateName(id, name);
        redirectAttributes.addFlashAttribute("successMessage", "Nombre actualizado");
        customerAuditLogger.logNameUpdate(id, form, authentication, request);

        return "redirect:/customers/" + id;
    }

    @GetMapping("/{id}/actions/name/edit")
    public String editName(@PathVariable Long id, Model model) {
        Customer customer = customerQueries.get(id);
        model.addAttribute("customer", customer);

        if (!model.containsAttribute("updateNameForm")) {
            String name = customer.getName();
            UpdateNameForm form = new UpdateNameForm(name);
            model.addAttribute("updateNameForm", form);
        }

        return "customers/actions/name-edit";
    }

    @GetMapping("/{id}/actions/sector/edit")
    public String editSector(@PathVariable Long id, Model model) {
        Customer customer = customerQueries.get(id);
        model.addAttribute("customer", customer);

        if (!model.containsAttribute("updateSectorForm")) {
            UpdateSectorForm form = new UpdateSectorForm();
            Sector sector = customer.getSector();
            Long sectorId = sector.getId();
            form.setSectorId(sectorId);
            model.addAttribute("updateSectorForm", form);
        }

        List<Sector> sectors = sectorService.listAll();
        model.addAttribute("sectors", sectors);

        return "customers/actions/sector-edit";
    }

    @PostMapping("/{id}/birthdate")
    public String updateBirthdate(
        @PathVariable Long id,
        @Valid @ModelAttribute("updateBirthdateForm") UpdateBirthdateForm form,
        BindingResult result,
        RedirectAttributes redirectAttributes,
        Authentication authentication,
        HttpServletRequest request
    ) {
        if (result.hasErrors()) {
            return CustomerFormRedirect.redirectToAction(id, redirectAttributes, "updateBirthdateForm", form, result, "birthdate/edit");
        }

        Integer day = form.getDay();
        Integer month = form.getMonth();
        Integer year = form.getYear();
        customerCommands.updateBirthdate(id, day, month, year);
        redirectAttributes.addFlashAttribute("successMessage", "Fecha de nacimiento actualizada");
        customerAuditLogger.logBirthdateUpdate(id, form, authentication, request);

        return "redirect:/customers/" + id;
    }

    @GetMapping("/{id}/actions/birthdate/edit")
    public String editBirthdate(@PathVariable Long id, Model model) {
        Customer customer = customerQueries.get(id);
        model.addAttribute("customer", customer);

        if (!model.containsAttribute("updateBirthdateForm")) {
            UpdateBirthdateForm form = new UpdateBirthdateForm();
            Integer day = customer.getBirthDay();
            form.setDay(day);
            Integer month = customer.getBirthMonth();
            form.setMonth(month);
            Integer year = customer.getBirthYear();
            form.setYear(year);
            model.addAttribute("updateBirthdateForm", form);
        }

        return "customers/actions/birthdate-edit";
    }
}
