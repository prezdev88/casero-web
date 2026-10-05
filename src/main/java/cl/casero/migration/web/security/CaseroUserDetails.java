package cl.casero.migration.web.security;

import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import cl.casero.migration.domain.enums.UserRole;
import cl.casero.migration.service.dto.UserIdentity;

@RequiredArgsConstructor
public class CaseroUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final UserIdentity identity;
    private final String username;
    private final String password;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        UserRole role = identity.role();
        String authorityName = "ROLE_" + role.name();
        GrantedAuthority authority = new SimpleGrantedAuthority(authorityName);
        return List.of(authority);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return identity.enabled();
    }

    public UserIdentity getIdentity() {
        return identity;
    }

    public boolean isAdmin() {
        return identity.isAdmin();
    }
}
