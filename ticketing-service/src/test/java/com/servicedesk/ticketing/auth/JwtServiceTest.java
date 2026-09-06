package com.servicedesk.ticketing.auth;

import com.servicedesk.ticketing.user.User;
import com.servicedesk.ticketing.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET_A = "a".repeat(32);
    private static final String SECRET_B = "b".repeat(32);

    private JwtService newJwtService(String secret, long expirationMinutes) {
        JwtService service = new JwtService(secret, expirationMinutes, "ticketing-service");
        ReflectionTestUtils.invokeMethod(service, "init");
        return service;
    }

    private User user(UserRole role) {
        return new User(UUID.randomUUID(), "Test User", "test@example.test", "hash", role, Instant.now());
    }

    @Test
    void issuedTokenDecodesWithMatchingClaims() {
        JwtService jwtService = newJwtService(SECRET_A, 60);
        User user = user(UserRole.AGENT);

        JwtService.IssuedToken issued = jwtService.issueFor(user);
        Jwt decoded = jwtService.decoder().decode(issued.accessToken());

        assertThat(jwtService.userId(decoded)).isEqualTo(user.getId());
        assertThat(jwtService.email(decoded)).isEqualTo(user.getEmail());
        assertThat(jwtService.fullName(decoded)).isEqualTo(user.getFullName());
        assertThat(jwtService.role(decoded)).isEqualTo(UserRole.AGENT);
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {
        JwtService issuer = newJwtService(SECRET_A, 60);
        JwtService verifier = newJwtService(SECRET_B, 60);

        String token = issuer.issueFor(user(UserRole.ADMIN)).accessToken();

        assertThatThrownBy(() -> verifier.decoder().decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void constructionFailsWhenSecretIsTooShort() {
        assertThatThrownBy(() -> newJwtService("too-short", 60))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructionFailsWhenSecretIsBlank() {
        assertThatThrownBy(() -> newJwtService("   ", 60))
                .isInstanceOf(IllegalStateException.class);
    }
}
