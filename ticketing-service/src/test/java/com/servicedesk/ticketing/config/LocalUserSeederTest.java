package com.servicedesk.ticketing.config;

import com.servicedesk.ticketing.user.User;
import com.servicedesk.ticketing.user.UserRepository;
import com.servicedesk.ticketing.user.UserRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalUserSeederTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void doesNothingWhenDisabled() {
        UserRepository userRepository = mock(UserRepository.class);
        LocalUserSeeder seeder = new LocalUserSeeder(userRepository, passwordEncoder, false, "", "", "");

        seeder.run(mock(ApplicationArguments.class));

        verify(userRepository, never()).save(any());
    }

    @Test
    void failsFastWhenEnabledWithoutAllPasswords() {
        UserRepository userRepository = mock(UserRepository.class);
        LocalUserSeeder seeder = new LocalUserSeeder(userRepository, passwordEncoder, true, "requester-pass", "", "admin-pass");

        assertThatThrownBy(() -> seeder.run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createsOneHashedUserPerRoleWhenEnabled() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        LocalUserSeeder seeder = new LocalUserSeeder(
                userRepository, passwordEncoder, true, "requester-pass", "agent-pass", "admin-pass");

        seeder.run(mock(ApplicationArguments.class));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.times(3)).save(captor.capture());

        List<User> saved = captor.getAllValues();
        assertThat(saved).extracting(User::getRole)
                .containsExactlyInAnyOrder(UserRole.REQUESTER, UserRole.AGENT, UserRole.ADMIN);
        assertThat(saved).allSatisfy(user -> assertThat(user.getPasswordHash()).isNotEqualTo(user.getEmail()));
        for (User user : saved) {
            assertThat(user.getPasswordHash()).doesNotContain("pass");
        }
    }

    @Test
    void doesNotOverwriteExistingUser() {
        UserRepository userRepository = mock(UserRepository.class);
        User existingRequester = new User(
                UUID.randomUUID(), "Existing Requester", "requester@example.test", "existing-hash",
                UserRole.REQUESTER, Instant.now());
        when(userRepository.findByEmail("requester@example.test")).thenReturn(Optional.of(existingRequester));
        when(userRepository.findByEmail("agent@example.test")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@example.test")).thenReturn(Optional.empty());

        LocalUserSeeder seeder = new LocalUserSeeder(
                userRepository, passwordEncoder, true, "requester-pass", "agent-pass", "admin-pass");

        seeder.run(mock(ApplicationArguments.class));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(User::getRole)
                .containsExactlyInAnyOrder(UserRole.AGENT, UserRole.ADMIN);
    }
}
