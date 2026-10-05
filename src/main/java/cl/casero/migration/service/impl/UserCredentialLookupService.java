package cl.casero.migration.service.impl;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.UserRole;
import cl.casero.migration.repository.AppUserRepository;
import cl.casero.migration.service.UserCredentialLookup;
import cl.casero.migration.service.dto.UserCredentials;
import cl.casero.migration.service.dto.UserIdentity;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCredentialLookupService implements UserCredentialLookup {

    private final AppUserRepository repository;

    @Override
    public Optional<UserCredentials> findByPinFingerprint(String fingerprint) {
        return repository.findByPinFingerprint(fingerprint).map(this::toCredentials);
    }

    private UserCredentials toCredentials(AppUser user) {
        Long id = user.getId();
        String name = user.getName();
        UserRole role = user.getRole();
        boolean enabled = user.isEnabled();
        UserIdentity identity = new UserIdentity(id, name, role, enabled);
        String fingerprint = user.getPinFingerprint();
        String hash = user.getPinHash();
        String salt = user.getPinSalt();
        return new UserCredentials(identity, fingerprint, hash, salt);
    }
}
