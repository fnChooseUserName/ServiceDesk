package com.servicedesk.ticketing.auth.dto;

import com.servicedesk.ticketing.user.UserRole;

import java.util.UUID;

public record AuthenticatedUserResponse(
        UUID id,
        String fullName,
        String email,
        UserRole role
) {
}
