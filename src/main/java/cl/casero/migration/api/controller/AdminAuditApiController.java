package cl.casero.migration.api.controller;

import cl.casero.migration.api.dto.AuditEventResponse;
import cl.casero.migration.api.dto.PageResponse;
import cl.casero.migration.api.dto.UserResponse;
import cl.casero.migration.domain.AuditEvent;
import cl.casero.migration.domain.enums.AuditEventType;
import cl.casero.migration.repository.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/audit")
public class AdminAuditApiController {

    private final AuditEventRepository auditEventRepository;

    public AdminAuditApiController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> listAuditEvents(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "eventType", required = false) String eventTypeParam,
            @RequestParam(value = "payloadType", required = false) String payloadType) {

        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, 
                Sort.by(Sort.Direction.DESC, "createdAt"));

        AuditEventType filterType = parseEventType(eventTypeParam);
        String sanitizedPayloadType = sanitize(payloadType);

        Page<AuditEvent> events = resolveEvents(pageable, filterType, sanitizedPayloadType);

        return PageResponse.of(events, events.getContent()
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public AuditEventResponse getAuditEvent(@PathVariable Long id) {
        return auditEventRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Audit event with id " + id + " not found"));
    }

    private Page<AuditEvent> resolveEvents(Pageable pageable, AuditEventType filterType, String sanitizedPayloadType) {
        boolean hasEventType = filterType != null;
        boolean hasPayloadType = sanitizedPayloadType != null && !sanitizedPayloadType.isBlank();

        if (hasEventType && hasPayloadType) {
            return auditEventRepository.findByEventTypeAndPayloadType(filterType, sanitizedPayloadType, pageable);
        } else if (hasEventType) {
            return auditEventRepository.findByEventTypeOrderByCreatedAtDesc(filterType, pageable);
        } else if (hasPayloadType) {
            return auditEventRepository.findByPayloadType(sanitizedPayloadType, pageable);
        } else {
            return auditEventRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
    }

    private AuditEventType parseEventType(String eventTypeParam) {
        if (eventTypeParam == null || eventTypeParam.isBlank()) {
            return null;
        }
        try {
            return AuditEventType.valueOf(eventTypeParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        return input.trim();
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        UserResponse userResponse = null;
        if (event.getUser() != null) {
            userResponse = new UserResponse(
                    event.getUser().getId(),
                    event.getUser().getName(),
                    event.getUser().getRole(),
                    event.getUser().isEnabled(),
                    event.getUser().getCreatedAt(),
                    event.getUser().getUpdatedAt()
            );
        }

        return new AuditEventResponse(
                event.getId(),
                event.getEventType().name(),
                userResponse,
                event.getPayload(),
                event.getIp(),
                event.getUserAgent(),
                event.getCreatedAt()
        );
    }
}
