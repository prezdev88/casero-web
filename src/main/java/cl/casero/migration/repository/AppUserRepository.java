package cl.casero.migration.repository;

import cl.casero.migration.domain.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByPinFingerprint(String pinFingerprint);

    List<AppUser> findAllByOrderByNameAsc();
}
