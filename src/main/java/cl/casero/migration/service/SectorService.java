package cl.casero.migration.service;

import cl.casero.migration.domain.Sector;

import java.util.List;

public interface SectorService {
    List<Sector> listAll();

    Sector get(Long id);
}
