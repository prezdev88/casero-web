package cl.casero.migration.web.audit;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Sector;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.service.AuditEventService;
import cl.casero.migration.service.dto.DebtForgivenessForm;
import cl.casero.migration.service.dto.MoneyTransactionForm;
import cl.casero.migration.service.dto.PaymentForm;
import cl.casero.migration.service.dto.SaleForm;
import cl.casero.migration.service.dto.UpdateAddressForm;
import cl.casero.migration.service.dto.UpdateBirthdateForm;
import cl.casero.migration.service.dto.UpdateNameForm;
import cl.casero.migration.service.dto.UpdateSectorForm;
import cl.casero.migration.web.security.CaseroUserDetails;

@Component
@RequiredArgsConstructor
public class CustomerAuditLogger {

    private final AuditEventService auditEventService;

    public void logCustomerCreated(
        Customer customer,
        Authentication authentication,
        HttpServletRequest request
    ) {
        Long customerId = customer.getId();
        String name = customer.getName();
        Sector sector = customer.getSector();
        Long sectorId = sector.getId();
        String rawAddress = customer.getAddress();
        String address = sanitize(rawAddress);

        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);
        data.put("name", name);
        data.put("sectorId", sectorId);
        data.put("address", address);

        logAction("CREATE_CUSTOMER", data, authentication, request);
    }

    public void logSale(
        Long customerId,
        SaleForm form,
        Authentication authentication,
        HttpServletRequest request
    ) {
        Integer amount = form.getAmount();
        Integer items = form.getItemsCount();
        LocalDate date = form.getDate();
        String formattedDate = dateToString(date);
        String rawDetail = form.getDetail();
        String detail = sanitize(rawDetail);

        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);
        data.put("amount", amount);
        data.put("items", items);
        data.put("date", formattedDate);
        data.put("detail", detail);

        logAction("SALE", data, authentication, request);
    }

    public void logPayment(
        Long customerId,
        PaymentForm form,
        Authentication authentication,
        HttpServletRequest request
    ) {
        Integer amount = form.getAmount();
        LocalDate date = form.getDate();
        String formattedDate = dateToString(date);

        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);
        data.put("amount", amount);
        data.put("date", formattedDate);

        logAction("PAYMENT", data, authentication, request);
    }

    public void logRefund(
        Long customerId,
        MoneyTransactionForm form,
        Authentication authentication,
        HttpServletRequest request
    ) {
        Integer amount = form.getAmount();
        LocalDate date = form.getDate();
        String formattedDate = dateToString(date);
        String rawDetail = form.getDetail();
        String detail = sanitize(rawDetail);

        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);
        data.put("amount", amount);
        data.put("date", formattedDate);
        data.put("detail", detail);

        logAction("REFUND", data, authentication, request);
    }

    public void logFaultDiscount(
        Long customerId,
        MoneyTransactionForm form,
        Authentication authentication,
        HttpServletRequest request
    ) {
        Integer amount = form.getAmount();
        LocalDate date = form.getDate();
        String formattedDate = dateToString(date);
        String rawDetail = form.getDetail();
        String detail = sanitize(rawDetail);

        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);
        data.put("amount", amount);
        data.put("date", formattedDate);
        data.put("detail", detail);

        logAction("FAULT_DISCOUNT", data, authentication, request);
    }

    public void logDebtForgiveness(
        Long customerId,
        DebtForgivenessForm form,
        Authentication authentication,
        HttpServletRequest request
    ) {
        LocalDate date = form.getDate();
        String formattedDate = dateToString(date);
        String rawDetail = form.getDetail();
        String detail = sanitize(rawDetail);

        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);
        data.put("date", formattedDate);
        data.put("detail", detail);

        logAction("DEBT_FORGIVEN", data, authentication, request);
    }

    public void logAddressUpdate(
        Long customerId,
        UpdateAddressForm form,
        Authentication authentication,
        HttpServletRequest request
    ) {
        String rawAddress = form.getNewAddress();
        String address = sanitize(rawAddress);

        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);
        data.put("address", address);

        logAction("UPDATE_CUSTOMER_ADDRESS", data, authentication, request);
    }

    public void logSectorUpdate(
        Long customerId,
        UpdateSectorForm form,
        Authentication authentication,
        HttpServletRequest request
    ) {
        Long sectorId = form.getSectorId();

        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);
        data.put("sectorId", sectorId);

        logAction("UPDATE_CUSTOMER_SECTOR", data, authentication, request);
    }

    public void logCustomerDeleted(
        Long customerId,
        Authentication authentication,
        HttpServletRequest request
    ) {

        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);

        logAction("DELETE_CUSTOMER", data, authentication, request);
    }

    public void logTransactionDeleted(
        Long transactionId,
        Long customerId,
        Authentication authentication,
        HttpServletRequest request
    ) {

        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", transactionId);
        data.put("customerId", customerId);

        logAction("TRANSACTION_DELETED", data, authentication, request);
    }

    public void logNameUpdate(
        Long customerId,
        UpdateNameForm form,
        Authentication authentication,
        HttpServletRequest request
    ) {
        String name = form.getNewName();

        Map<String, Object> data = new HashMap<>();
        data.put("action", "UPDATE_CUSTOMER_NAME");
        data.put("customerId", customerId);
        data.put("name", name);

        logEvent(data, authentication, request);
    }

    public void logBirthdateUpdate(
        Long customerId,
        UpdateBirthdateForm form,
        Authentication authentication,
        HttpServletRequest request
    ) {
        Integer day = form.getDay();
        Integer month = form.getMonth();
        Integer year = form.getYear();

        Map<String, Object> data = new HashMap<>();
        data.put("action", "UPDATE_CUSTOMER_BIRTHDATE");
        data.put("customerId", customerId);
        data.put("day", day);
        data.put("month", month);
        data.put("year", year);

        logEvent(data, authentication, request);
    }

    private void logAction(
        String actionType,
        Map<String, Object> data,
        Authentication authentication,
        HttpServletRequest request
    ) {
        data.values().removeIf(Objects::isNull);
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", actionType);
        payload.put("data", data);
        logEvent(payload, authentication, request);
    }

    private void logEvent(
        Map<String, Object> payload,
        Authentication authentication,
        HttpServletRequest request
    ) {
        payload.values().removeIf(Objects::isNull);
        AppUser actor = currentUser(authentication);
        auditEventService.logEvent(AuditEventType.ACTION, actor, payload, request);
    }

    private AppUser currentUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CaseroUserDetails details) {
            return details.getAppUser();
        }

        return null;
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("\\s+", " ").trim();
    }

    private String dateToString(LocalDate date) {
        return (date != null) ? date.toString() : null;
    }
}
