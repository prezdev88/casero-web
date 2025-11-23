package cl.casero.migration.web.security;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.service.AppUserService;
import cl.casero.migration.util.PinHasher;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class PinAuthenticationProvider implements AuthenticationProvider {

    private final AppUserService userService;
    private final PinHasher pinHasher;

    public PinAuthenticationProvider(AppUserService userService, PinHasher pinHasher) {
        this.userService = userService;
        this.pinHasher = pinHasher;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof PinAuthenticationToken token)) {
            return null;
        }

        String rawPin = (String) token.getPrincipal();
        String fingerprint = pinHasher.fingerprint(rawPin);
        AppUser user = userService.findByPinFingerprint(fingerprint)
                .orElseThrow(() -> new BadCredentialsException("PIN incorrecto"));

        if (!user.isEnabled()) {
            throw new DisabledException("Usuario deshabilitado");
        }

        if (!pinHasher.matches(rawPin, user.getPinSalt(), user.getPinHash())) {
            throw new BadCredentialsException("PIN incorrecto");
        }

        CaseroUserDetails userDetails = new CaseroUserDetails(user);
        PinAuthenticationToken authenticated = new PinAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());
        authenticated.setDetails(token.getDetails());
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PinAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
