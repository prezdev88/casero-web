package cl.casero.migration.web.controller;

import cl.casero.migration.domain.AuditEvent;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.repository.AuditEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AdminAuditController {

    private final AuditEventRepository auditEventRepository;

    @GetMapping("/admin/audit")
    @Transactional(readOnly = true)
    public String audit(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "eventType", required = false) String eventTypeParam,
        @RequestParam(value = "payloadType", required = false) String payloadType,
        Model model
    ) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        AuditEventType filterType = parseEventType(eventTypeParam);
        String sanitizedPayloadType = sanitize(payloadType);

        var events = resolveEvents(pageable, filterType, sanitizedPayloadType);

        model.addAttribute("events", events);
        model.addAttribute("page", sanitizedPage);
        model.addAttribute("size", sanitizedSize);
        model.addAttribute("eventTypes", AuditEventType.values());
        model.addAttribute("selectedEventType", filterType);
        model.addAttribute("payloadType", sanitizedPayloadType);
        model.addAttribute("payloadTypeOptions", payloadTypeOptions());

        return "admin/audit";
    }

    private Page<AuditEvent> resolveEvents(Pageable pageable, AuditEventType filterType, String payloadType) {
        if (payloadType != null && filterType != null) {
            return auditEventRepository.findByEventTypeAndPayloadType(filterType, payloadType, pageable);
        }
        if (payloadType != null) {
            return auditEventRepository.findByPayloadType(payloadType, pageable);
        }
        if (filterType != null) {
            return auditEventRepository.findByEventTypeOrderByCreatedAtDesc(filterType, pageable);
        }
        return auditEventRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    private AuditEventType parseEventType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AuditEventType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> payloadTypeOptions() {
        return List.of(
            "DEBT_FORGIVEN",
            "SALE",
            "PAYMENT",
            "REFUND",
            "DISCOUNT",
            "UPDATE"
        );
    }
}
