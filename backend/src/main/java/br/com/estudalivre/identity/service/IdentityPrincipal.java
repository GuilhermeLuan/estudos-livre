package br.com.estudalivre.identity.service;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class IdentityPrincipal implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String email;
    private final String timeZone;
    private String password;

    public IdentityPrincipal(UUID id, String email, String password, String timeZone) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.timeZone = timeZone;
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String timeZone() {
        return timeZone;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void eraseCredentials() {
        password = null;
    }
}
