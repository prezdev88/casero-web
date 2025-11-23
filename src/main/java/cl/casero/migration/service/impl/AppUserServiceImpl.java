package cl.casero.migration.service.impl;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.UserRole;
import cl.casero.migration.repository.AppUserRepository;
import cl.casero.migration.service.AppUserService;
import cl.casero.migration.util.PinHasher;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AppUserServiceImpl implements AppUserService {

    private final AppUserRepository repository;
    private final PinHasher pinHasher;

    public AppUserServiceImpl(AppUserRepository repository, PinHasher pinHasher) {
        this.repository = repository;
        this.pinHasher = pinHasher;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppUser> findByPinFingerprint(String pinFingerprint) {
        return repository.findByPinFingerprint(pinFingerprint);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppUser> listAll() {
        return repository.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional
    public AppUser create(String name, UserRole role, String pin) {
        String sanitizedName = sanitizeName(name);
        String sanitizedPin = sanitizePin(pin);
        String salt = pinHasher.generateSalt();
        String hash = pinHasher.hashWithSalt(sanitizedPin, salt);
        String fingerprint = pinHasher.fingerprint(sanitizedPin);

        repository.findByPinFingerprint(fingerprint).ifPresent(existing -> {
            throw new IllegalArgumentException("Ya existe un usuario con ese PIN");
        });

        AppUser user = new AppUser();
        user.setName(sanitizedName);
        user.setRole(role);
        user.setPinSalt(salt);
        user.setPinHash(hash);
        user.setPinFingerprint(fingerprint);
        return repository.save(user);
    }

    @Override
    @Transactional
    public void updatePin(Long userId, String pin) {
        AppUser user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String sanitizedPin = sanitizePin(pin);
        String salt = pinHasher.generateSalt();
        String hash = pinHasher.hashWithSalt(sanitizedPin, salt);
        String fingerprint = pinHasher.fingerprint(sanitizedPin);

        repository.findByPinFingerprint(fingerprint)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya existe un usuario con ese PIN");
                });

        user.setPinSalt(salt);
        user.setPinHash(hash);
        user.setPinFingerprint(fingerprint);
    }

    private String sanitizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        return name.trim();
    }

    private String sanitizePin(String pin) {
        if (!StringUtils.hasText(pin)) {
            throw new IllegalArgumentException("El PIN es obligatorio");
        }
        String normalized = pin.trim();
        if (!normalized.matches("\\d{4,12}")) {
            throw new IllegalArgumentException("El PIN debe tener entre 4 y 12 dígitos");
        }
        return normalized;
    }
}
