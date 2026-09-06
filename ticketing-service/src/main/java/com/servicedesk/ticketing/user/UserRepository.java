package com.servicedesk.ticketing.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Looks up a user by email. Callers must pass an already-normalized
     * (lowercased, trimmed) email since emails are stored normalized.
     */
    Optional<User> findByEmail(String email);
}
