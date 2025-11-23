package cl.casero.migration.web.security;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.UserRole;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CaseroUserDetails implements UserDetails {

    private final AppUser user;

    public CaseroUserDetails(AppUser user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPinHash();
    }

    @Override
    public String getUsername() {
        return user.getPinFingerprint();
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
        return user.isEnabled();
    }

    public AppUser getUser() {
        return user;
    }

    public boolean isAdmin() {
        return user.getRole() == UserRole.ADMIN;
    }
}
