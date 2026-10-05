package cl.casero.migration.service;

import java.util.Optional;

import cl.casero.migration.domain.AppUser;

public interface UserCredentialLookup {
    Optional<AppUser> findByPinFingerprint(String fingerprint);
}
