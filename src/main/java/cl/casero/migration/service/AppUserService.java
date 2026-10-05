package cl.casero.migration.service;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.UserRole;
import java.util.List;

public interface AppUserService {
    List<AppUser> listAll();

    AppUser create(String name, UserRole role, String pin);

    void updatePin(Long userId, String pin);
}
