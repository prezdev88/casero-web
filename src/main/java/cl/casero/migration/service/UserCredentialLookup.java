package cl.casero.migration.service;

import java.util.Optional;

import cl.casero.migration.service.dto.UserCredentials;

public interface UserCredentialLookup {
    Optional<UserCredentials> findByPinFingerprint(String fingerprint);
}
