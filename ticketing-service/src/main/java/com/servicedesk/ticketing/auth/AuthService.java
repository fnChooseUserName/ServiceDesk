package com.servicedesk.ticketing.auth;

import com.servicedesk.ticketing.auth.dto.AuthenticatedUserResponse;
import com.servicedesk.ticketing.auth.dto.LoginResponse;
import com.servicedesk.ticketing.user.DatabaseUserDetailsService;
import com.servicedesk.ticketing.user.User;
import com.servicedesk.ticketing.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Verifies login credentials and issues bearer tokens. Failures are
 * deliberately collapsed into a single {@link InvalidCredentialsException}
 * regardless of whether the email is unknown or the password is wrong.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse login(String email, String password) {
        String normalizedEmail = DatabaseUserDetailsService.normalize(email);

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedEmail, password));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }

        // Authentication succeeded, so the user is guaranteed to exist.
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        JwtService.IssuedToken issuedToken = jwtService.issueFor(user);
        return new LoginResponse(
                issuedToken.accessToken(),
                "Bearer",
                issuedToken.expiresAt(),
                toResponse(user)
        );
    }

    private static AuthenticatedUserResponse toResponse(User user) {
        return new AuthenticatedUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
