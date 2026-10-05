package cl.casero.migration.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import cl.casero.migration.service.CustomerScoreService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/customers")
public class CustomerRankingController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CustomerScoreService customerScoreService;

    @GetMapping("/ranking")
    public String ranking(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "100") int size,
        @RequestParam(value = "direction", defaultValue = "desc") String direction,
        Model model
    ) {
        int sanitizedPage = Math.max(page, 0);
        int positiveSize = Math.max(size, 1);
        int sanitizedSize = Math.min(positiveSize, MAX_PAGE_SIZE);
        boolean ascending = "asc".equalsIgnoreCase(direction);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize);
        Page<CustomerScoreService.RankingEntry> rankingPage = customerScoreService.getRanking(pageable, ascending);

        model.addAttribute("rankingPage", rankingPage);
        model.addAttribute("direction", ascending ? "asc" : "desc");
        model.addAttribute("nextDirection", ascending ? "desc" : "asc");

        return "customers/ranking";
    }
}
