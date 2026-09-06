package com.servicedesk.ticketing.user;

/**
 * The three fixed roles defined by the domain model. Every {@link User}
 * has exactly one role.
 */
public enum UserRole {
    REQUESTER,
    AGENT,
    ADMIN
}
