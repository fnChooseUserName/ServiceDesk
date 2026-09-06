package com.servicedesk.ticketing.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.servicedesk.ticketing.user.User;
import com.servicedesk.ticketing.user.UserRole;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Issues and validates the signed JWT bearer tokens used as this service's
 * only authentication mechanism (see
 * docs/decisions/0003-authentication-and-identity-lifecycle.md). Tokens are
 * self-contained: the subject, email, full name, and role claims are all a
 * caller needs, so verifying a token never requires a database lookup.
 */
@Component
public class JwtService {

    /** Minimum HMAC-SHA-256 key length in bytes (256 bits). */
    private static final int MIN_SECRET_BYTES = 32;

    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_FULL_NAME = "fullName";
    public static final String CLAIM_ROLE = "role";

    private final String issuer;
    private final long expirationMinutes;
    private final byte[] secretBytes;

    private JwtEncoder jwtEncoder;
    private JwtDecoder jwtDecoder;
    private JwsHeader jwsHeader;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes,
            @Value("${app.jwt.issuer}") String issuer
    ) {
        this.expirationMinutes = expirationMinutes;
        this.issuer = issuer;
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) must be set to a non-blank value of at least "
                            + MIN_SECRET_BYTES + " bytes");
        }
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) must be at least " + MIN_SECRET_BYTES
                            + " bytes long for HS256; got " + this.secretBytes.length);
        }
    }

    @PostConstruct
    void init() {
        SecretKeySpec secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        this.jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    public JwtDecoder decoder() {
        return jwtDecoder;
    }

    public record IssuedToken(String accessToken, Instant expiresAt) {
    }

    public IssuedToken issueFor(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_FULL_NAME, user.getFullName())
                .claim(CLAIM_ROLE, user.getRole().name())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public String email(Jwt jwt) {
        return jwt.getClaimAsString(CLAIM_EMAIL);
    }

    public String fullName(Jwt jwt) {
        return jwt.getClaimAsString(CLAIM_FULL_NAME);
    }

    public UserRole role(Jwt jwt) {
        return UserRole.valueOf(jwt.getClaimAsString(CLAIM_ROLE));
    }
}
