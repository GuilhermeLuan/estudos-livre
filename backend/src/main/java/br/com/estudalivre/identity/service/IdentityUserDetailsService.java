package br.com.estudalivre.identity.service;

import java.util.Locale;

import br.com.estudalivre.identity.repository.IdentityUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class IdentityUserDetailsService implements UserDetailsService {

    private final IdentityUserRepository identityUserRepository;

    public IdentityUserDetailsService(IdentityUserRepository identityUserRepository) {
        this.identityUserRepository = identityUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        return identityUserRepository.findByEmail(normalizedEmail)
                .map(user -> new IdentityPrincipal(
                        user.id(), user.email(), user.passwordHash(), user.timeZone()))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
    }
}
