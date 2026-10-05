package cl.casero.migration.web.security;

import java.util.Collection;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.service.UserCredentialLookup;
import cl.casero.migration.util.PinHasher;

@Component
@RequiredArgsConstructor
public class PinAuthenticationProvider implements AuthenticationProvider {

    private final PinHasher pinHasher;
    private final UserCredentialLookup credentialLookup;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof PinAuthenticationToken token)) {
            return null;
        }

        String rawPin = (String) token.getPrincipal();
        String fingerprint = pinHasher.fingerprint(rawPin);
        AppUser user = credentialLookup.findByPinFingerprint(fingerprint)
                .orElseThrow(() -> new BadCredentialsException("PIN incorrecto"));

        if (!user.isEnabled()) {
            throw new DisabledException("Usuario deshabilitado");
        }

        String salt = user.getPinSalt();
        String hash = user.getPinHash();
        if (!pinHasher.matches(rawPin, salt, hash)) {
            throw new BadCredentialsException("PIN incorrecto");
        }

        CaseroUserDetails userDetails = new CaseroUserDetails(user);
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        PinAuthenticationToken authenticated = new PinAuthenticationToken(
                userDetails,
                null,
                authorities);

        Object details = token.getDetails();
        authenticated.setDetails(details);
        
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PinAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
