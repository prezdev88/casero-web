package cl.casero.migration.service.impl;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.repository.AppUserRepository;
import cl.casero.migration.service.UserCredentialLookup;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCredentialLookupService implements UserCredentialLookup {

    private final AppUserRepository repository;

    @Override
    public Optional<AppUser> findByPinFingerprint(String fingerprint) {
        return repository.findByPinFingerprint(fingerprint);
    }
}
