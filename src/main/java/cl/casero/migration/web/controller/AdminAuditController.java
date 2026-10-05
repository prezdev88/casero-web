package cl.casero.migration.web.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import cl.casero.migration.domain.AuditEvent;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.service.AuditQueries;
import cl.casero.migration.service.audit.AuditAction;
import cl.casero.migration.service.dto.AuditSearchCriteria;

@Controller
@RequiredArgsConstructor
public class AdminAuditController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditQueries auditQueries;

    @GetMapping("/admin/audit")
    public String audit(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "eventType", required = false) String eventTypeParam,
        @RequestParam(value = "payloadType", required = false) String payloadType,
        Model model
    ) {
        int sanitizedPage = Math.max(page, 0);
        int positiveSize = Math.max(size, 1);
        int sanitizedSize = Math.min(positiveSize, MAX_PAGE_SIZE);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);
        AuditEventType filterType = parseEventType(eventTypeParam);
        String sanitizedPayloadType = sanitize(payloadType);

        AuditSearchCriteria criteria = new AuditSearchCriteria(filterType, sanitizedPayloadType, pageable);
        Page<AuditEvent> events = auditQueries.search(criteria);

        model.addAttribute("events", events);
        model.addAttribute("page", sanitizedPage);
        model.addAttribute("size", sanitizedSize);
        AuditEventType[] eventTypes = AuditEventType.values();
        model.addAttribute("eventTypes", eventTypes);
        model.addAttribute("selectedEventType", filterType);
        model.addAttribute("payloadType", sanitizedPayloadType);
        List<String> payloadTypes = AuditAction.payloadTypeOptions();
        model.addAttribute("payloadTypeOptions", payloadTypes);

        return "admin/audit";
    }

    private AuditEventType parseEventType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (AuditEventType eventType : AuditEventType.values()) {
            String name = eventType.name();
            if (name.equals(value)) {
                return eventType;
            }
        }

        return null;
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
