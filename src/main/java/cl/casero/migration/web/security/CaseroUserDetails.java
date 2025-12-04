package cl.casero.migration.web.security;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.enums.UserRole;
import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@AllArgsConstructor
public class CaseroUserDetails implements UserDetails {

    private final AppUser appUser;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()));
    }

    @Override
    public String getPassword() {
        return appUser.getPinHash();
    }

    @Override
    public String getUsername() {
        return appUser.getPinFingerprint();
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
        return appUser.isEnabled();
    }

    public AppUser getAppUser() {
        return appUser;
    }

    public boolean isAdmin() {
        return appUser.getRole() == UserRole.ADMIN;
    }
}
