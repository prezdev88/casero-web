package cl.casero.migration.service.impl;

import cl.casero.migration.domain.Sector;
import cl.casero.migration.repository.SectorRepository;
import cl.casero.migration.service.SectorService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class SectorServiceImpl implements SectorService {

    private final SectorRepository sectorRepository;

    public SectorServiceImpl(SectorRepository sectorRepository) {
        this.sectorRepository = sectorRepository;
    }

    @Override
    public List<Sector> listAll() {
        return sectorRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Sector::getName))
                .toList();
    }

    @Override
    public Sector get(Long id) {
        return sectorRepository.findById(id).orElseThrow();
    }
}
