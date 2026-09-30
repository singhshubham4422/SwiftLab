package com.example.invoiceapp.security;

import com.example.invoiceapp.model.User;
import com.example.invoiceapp.model.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserPrincipal implements UserDetails {
    private final Long userId;
    private final Long organizationId;
    private final String email;
    private final String password;
    private final String fullName;
    private final UserRole role;
    private final boolean active;

    public AppUserPrincipal(User user) {
        this.userId = user.getId();
        this.organizationId = user.getOrganizationId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.fullName = user.getFullName();
        this.role = user.getRole() != null ? user.getRole() : UserRole.STAFF;
        this.active = user.isActive();
    }

    public Long getUserId() { return userId; }
    public Long getOrganizationId() { return organizationId; }
    public String getFullName() { return fullName; }
    public UserRole getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return active; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return active; }
}
