package cl.casero.migration.web.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;

public class PinAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private Object credentials;

    public PinAuthenticationToken(String pin) {
        super(null);
        this.principal = pin;
        this.credentials = pin;
        setAuthenticated(false);
    }

    public PinAuthenticationToken(Object principal,
                                  Object credentials,
                                  Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        credentials = null;
    }
}
