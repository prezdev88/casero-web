package cl.casero.migration.web.security;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.service.AppUserService;
import cl.casero.migration.util.PinHasher;
import lombok.AllArgsConstructor;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PinAuthenticationProvider implements AuthenticationProvider {

    private final PinHasher pinHasher;
    private final AppUserService userService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof PinAuthenticationToken token)) {
            return null;
        }

        String rawPin = (String) token.getPrincipal();
        String fingerprint = pinHasher.fingerprint(rawPin);
        CaseroUserDetails userDetails = loadUserDetails(fingerprint);
        if (!pinHasher.matches(rawPin, userDetails.getUser().getPinSalt(), userDetails.getUser().getPinHash())) {
            throw new BadCredentialsException("PIN incorrecto");
        }
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

    CaseroUserDetails loadUserDetails(String fingerprint) {
        AppUser user = userService.findByPinFingerprint(fingerprint)
                .orElseThrow(() -> new UsernameNotFoundException("PIN incorrecto"));
        if (!user.isEnabled()) {
            throw new DisabledException("Usuario deshabilitado");
        }
        return new CaseroUserDetails(user);
    }
}
