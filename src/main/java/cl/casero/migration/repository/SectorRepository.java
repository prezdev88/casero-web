package cl.casero.migration.repository;

import cl.casero.migration.domain.Sector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    Optional<Sector> findByNameIgnoreCase(String name);
}
