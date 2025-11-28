package cl.casero.migration.api.controller;

import cl.casero.migration.api.dto.SectorResponse;
import cl.casero.migration.service.SectorService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sectors")
public class SectorApiController {

    private final SectorService sectorService;

    public SectorApiController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @GetMapping
    public List<SectorResponse> list() {
        return sectorService.listAll()
                .stream()
                .map(sector -> new SectorResponse(sector.getId(), sector.getName()))
                .toList();
    }
}
