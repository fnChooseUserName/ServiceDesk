package com.servicedesk.ticketing.auth.dto;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        AuthenticatedUserResponse user
) {
}
