package cl.casero.migration.web.controller;

import cl.casero.migration.repository.AuditEventRepository;
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
        Model model
    ) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        var events = auditEventRepository.findAllByOrderByCreatedAtDesc(pageable);

        model.addAttribute("events", events);
        model.addAttribute("page", sanitizedPage);
        model.addAttribute("size", sanitizedSize);

        return "admin/audit";
    }
}
