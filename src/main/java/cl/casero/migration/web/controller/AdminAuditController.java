package cl.casero.migration.web.controller;

import cl.casero.migration.repository.AuditEventRepository;
import cl.casero.migration.domain.enums.AuditEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AdminAuditController {

    private final AuditEventRepository auditEventRepository;

    @GetMapping("/admin/audit")
    public String audit(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "eventType", required = false) String eventTypeParam,
        Model model
    ) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        AuditEventType filterType = parseEventType(eventTypeParam);
        var events = filterType == null
            ? auditEventRepository.findAllByOrderByCreatedAtDesc(pageable)
            : auditEventRepository.findByEventTypeOrderByCreatedAtDesc(filterType, pageable);

        model.addAttribute("events", events);
        model.addAttribute("page", sanitizedPage);
        model.addAttribute("size", sanitizedSize);
        model.addAttribute("eventTypes", AuditEventType.values());
        model.addAttribute("selectedEventType", filterType);

        return "admin/audit";
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
}
