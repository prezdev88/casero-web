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
import cl.casero.migration.service.audit.AuditAction;
import cl.casero.migration.service.dto.AuditContext;
import cl.casero.migration.web.form.DebtForgivenessForm;
import cl.casero.migration.web.form.MoneyTransactionForm;
import cl.casero.migration.web.form.PaymentForm;
import cl.casero.migration.web.form.SaleForm;
import cl.casero.migration.service.dto.UpdateAddressForm;
import cl.casero.migration.service.dto.UpdateBirthdateForm;
import cl.casero.migration.service.dto.UpdateNameForm;
import cl.casero.migration.service.dto.UpdateSectorForm;
import cl.casero.migration.web.security.CaseroUserDetails;

@Component
@RequiredArgsConstructor
public class CustomerAuditLogger {

    private final AuditEventService auditEventService;
    private final AuditContextFactory auditContextFactory;

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

        logAction(AuditAction.CREATE_CUSTOMER, data, authentication, request);
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

        logAction(AuditAction.SALE, data, authentication, request);
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

        logAction(AuditAction.PAYMENT, data, authentication, request);
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

        logAction(AuditAction.REFUND, data, authentication, request);
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

        logAction(AuditAction.FAULT_DISCOUNT, data, authentication, request);
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

        logAction(AuditAction.DEBT_FORGIVEN, data, authentication, request);
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

        logAction(AuditAction.UPDATE_CUSTOMER_ADDRESS, data, authentication, request);
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

        logAction(AuditAction.UPDATE_CUSTOMER_SECTOR, data, authentication, request);
    }

    public void logCustomerDeleted(
        Long customerId,
        Authentication authentication,
        HttpServletRequest request
    ) {

        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);

        logAction(AuditAction.DELETE_CUSTOMER, data, authentication, request);
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

        logAction(AuditAction.TRANSACTION_DELETED, data, authentication, request);
    }

    public void logNameUpdate(
        Long customerId,
        UpdateNameForm form,
        Authentication authentication,
        HttpServletRequest request
    ) {
        String name = form.getNewName();

        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);
        data.put("name", name);

        Map<String, Object> payload = AuditAction.UPDATE_CUSTOMER_NAME.payload(data);
        logEvent(payload, authentication, request);
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
        data.put("customerId", customerId);
        data.put("day", day);
        data.put("month", month);
        data.put("year", year);

        Map<String, Object> payload = AuditAction.UPDATE_CUSTOMER_BIRTHDATE.payload(data);
        logEvent(payload, authentication, request);
    }

    private void logAction(
        AuditAction actionType,
        Map<String, Object> data,
        Authentication authentication,
        HttpServletRequest request
    ) {
        Map<String, Object> payload = actionType.payload(data);
        logEvent(payload, authentication, request);
    }

    private void logEvent(
        Map<String, Object> payload,
        Authentication authentication,
        HttpServletRequest request
    ) {
        payload.values().removeIf(Objects::isNull);
        AppUser actor = currentUser(authentication);
        AuditContext context = auditContextFactory.from(actor, request);
        auditEventService.logEvent(AuditEventType.ACTION, payload, context);
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
