package com.nexora.infrastructure.security;

import com.nexora.domain.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Adapta o User do domínio para a interface UserDetails do Spring Security.
 * Isola o domínio do framework — Spring Security nunca toca em User diretamente.
 */
public class NexoraUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String role;
    private final boolean active;

    public NexoraUserDetails(User user) {
        this.id           = user.getId();
        this.email        = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role         = user.getRole().name();
        this.active       = user.isActive();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Prefixo ROLE_ é convenção do Spring Security
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public String  getPassword()                { return passwordHash; }
    @Override public String  getUsername()                { return email; }
    @Override public boolean isAccountNonExpired()        { return true; }
    @Override public boolean isAccountNonLocked()         { return true; }
    @Override public boolean isCredentialsNonExpired()    { return true; }
    @Override public boolean isEnabled()                  { return active; }

    public UUID   getId()   { return id; }
    public String getRole() { return role; }
}