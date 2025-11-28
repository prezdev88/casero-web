package cl.casero.migration.service;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.UserRole;
import java.util.List;
import java.util.Optional;

public interface AppUserService {
    Optional<AppUser> findByPinFingerprint(String pinFingerprint);

    List<AppUser> listAll();

    AppUser create(String name, UserRole role, String pin);

    void updatePin(Long userId, String pin);
}
