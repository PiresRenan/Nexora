package com.nexora.infrastructure.security;

import com.nexora.domain.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carrega UserDetails a partir do repositório de domínio.
 * Usado pelo Spring Security para autenticação via email+senha.
 */
@Service
public class NexoraUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public NexoraUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email.toLowerCase())
                .map(NexoraUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}