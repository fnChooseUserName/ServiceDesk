package com.servicedesk.ticketing.user;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads users for Spring Security's authentication machinery (used by
 * {@code AuthenticationManager} during login). Authorization for already
 * authenticated requests is derived from JWT claims instead, see
 * {@code com.servicedesk.ticketing.auth.SecurityConfig}.
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("No user with the given email"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
