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

import cl.casero.migration.service.UserCredentialLookup;
import cl.casero.migration.service.dto.UserCredentials;
import cl.casero.migration.service.dto.UserIdentity;
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
        UserCredentials credentials = credentialLookup.findByPinFingerprint(fingerprint)
                .orElseThrow(() -> new BadCredentialsException("PIN incorrecto"));

        UserIdentity identity = credentials.identity();
        if (!identity.enabled()) {
            throw new DisabledException("Usuario deshabilitado");
        }

        String salt = credentials.pinSalt();
        String hash = credentials.pinHash();
        if (!pinHasher.matches(rawPin, salt, hash)) {
            throw new BadCredentialsException("PIN incorrecto");
        }

        String username = credentials.pinFingerprint();
        CaseroUserDetails userDetails = new CaseroUserDetails(identity, username, hash);
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
