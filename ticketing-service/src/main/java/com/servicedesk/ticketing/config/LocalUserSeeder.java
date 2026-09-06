package com.servicedesk.ticketing.config;

import com.servicedesk.ticketing.user.User;
import com.servicedesk.ticketing.user.UserRepository;
import com.servicedesk.ticketing.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Creates one local test identity per role (REQUESTER, AGENT, ADMIN) so S2's
 * acceptance criteria can be exercised without self-registration or a
 * committed migration containing credentials. Only runs under the "local"
 * Spring profile and only when explicitly enabled, and never overwrites an
 * account that already exists.
 *
 * See README.md "Database migrations and seed data" for how to supply the
 * required environment variables.
 */
@Component
@Profile("local")
public class LocalUserSeeder implements ApplicationRunner {

    private record SeedUser(String email, String fullName, UserRole role, String password) {
    }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String requesterPassword;
    private final String agentPassword;
    private final String adminPassword;

    public LocalUserSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.enabled:false}") boolean enabled,
            @Value("${app.seed.requester-password:}") String requesterPassword,
            @Value("${app.seed.agent-password:}") String agentPassword,
            @Value("${app.seed.admin-password:}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.requesterPassword = requesterPassword;
        this.agentPassword = agentPassword;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        if (requesterPassword.isBlank() || agentPassword.isBlank() || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "SEED_USERS_ENABLED is true but one or more of SEED_REQUESTER_PASSWORD, "
                            + "SEED_AGENT_PASSWORD, SEED_ADMIN_PASSWORD is missing");
        }

        List<SeedUser> seedUsers = List.of(
                new SeedUser("requester@example.test", "Local Requester", UserRole.REQUESTER, requesterPassword),
                new SeedUser("agent@example.test", "Local Agent", UserRole.AGENT, agentPassword),
                new SeedUser("admin@example.test", "Local Admin", UserRole.ADMIN, adminPassword)
        );

        for (SeedUser seedUser : seedUsers) {
            if (userRepository.findByEmail(seedUser.email()).isPresent()) {
                continue;
            }
            userRepository.save(new User(
                    UUID.randomUUID(),
                    seedUser.fullName(),
                    seedUser.email(),
                    passwordEncoder.encode(seedUser.password()),
                    seedUser.role(),
                    Instant.now()
            ));
        }
    }
}
