package com.servicedesk.ticketing.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmailReturnsPersistedUser() {
        User user = new User(UUID.randomUUID(), "Jane Agent", "jane@example.test", "hash", UserRole.AGENT, Instant.now());
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("jane@example.test");

        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(UserRole.AGENT);
        assertThat(found.get().getFullName()).isEqualTo("Jane Agent");
    }

    @Test
    void findByEmailReturnsEmptyForUnknownEmail() {
        assertThat(userRepository.findByEmail("nobody@example.test")).isEmpty();
    }

    @Test
    void duplicateEmailIsRejected() {
        userRepository.save(new User(UUID.randomUUID(), "First", "dup@example.test", "hash", UserRole.REQUESTER, Instant.now()));
        userRepository.flush();

        assertThatThrownBy(() -> {
            userRepository.save(new User(UUID.randomUUID(), "Second", "dup@example.test", "hash", UserRole.ADMIN, Instant.now()));
            userRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
