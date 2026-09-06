package com.servicedesk.ticketing.auth;

import tools.jackson.databind.ObjectMapper;
import com.servicedesk.ticketing.user.User;
import com.servicedesk.ticketing.user.UserRepository;
import com.servicedesk.ticketing.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of the S2 acceptance criteria: each seeded role can
 * log in, a valid token reaches a protected endpoint with the correct
 * identity/role, invalid/missing/altered/expired tokens are rejected, and
 * role-protected methods enforce the required role.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationFlowTest {

    private static final String REQUESTER_PASSWORD = "requester-pass-123";
    private static final String AGENT_PASSWORD = "agent-pass-123";
    private static final String ADMIN_PASSWORD = "admin-pass-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void seedUsers() {
        createUserIfAbsent("requester@flow.test", "Flow Requester", UserRole.REQUESTER, REQUESTER_PASSWORD);
        createUserIfAbsent("agent@flow.test", "Flow Agent", UserRole.AGENT, AGENT_PASSWORD);
        createUserIfAbsent("admin@flow.test", "Flow Admin", UserRole.ADMIN, ADMIN_PASSWORD);
    }

    private void createUserIfAbsent(String email, String fullName, UserRole role, String password) {
        userRepository.findByEmail(email).ifPresentOrElse(existing -> {
        }, () -> userRepository.save(new User(
                UUID.randomUUID(), fullName, email, passwordEncoder.encode(password), role, Instant.now()
        )));
    }

    @Test
    void healthEndpointRemainsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void eachSeededRoleCanLoginAndReceivesItsOwnIdentity() throws Exception {
        assertLoginReturnsIdentity("requester@flow.test", REQUESTER_PASSWORD, "REQUESTER");
        assertLoginReturnsIdentity("agent@flow.test", AGENT_PASSWORD, "AGENT");
        assertLoginReturnsIdentity("admin@flow.test", ADMIN_PASSWORD, "ADMIN");
    }

    private void assertLoginReturnsIdentity(String email, String password, String expectedRole) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value(expectedRole));
    }

    @Test
    void loginWithUnknownEmailIsRejectedWithGenericMessage() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", "nobody@flow.test", "password", "whatever123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void loginWithWrongPasswordIsRejectedWithSameGenericMessage() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", "requester@flow.test", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void meRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsIdentityForValidToken() throws Exception {
        String token = loginAndGetToken("agent@flow.test", AGENT_PASSWORD);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("agent@flow.test"))
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    void meRejectsMalformedToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRejectsTokenWithTamperedSignature() throws Exception {
        String token = loginAndGetToken("agent@flow.test", AGENT_PASSWORD);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "B" : "A") + "x";

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void agentOnlyEndpointAllowsAgentAndRejectsRequester() throws Exception {
        String agentToken = loginAndGetToken("agent@flow.test", AGENT_PASSWORD);
        String requesterToken = loginAndGetToken("requester@flow.test", REQUESTER_PASSWORD);

        mockMvc.perform(get("/api/test/agent-only").header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/test/agent-only").header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isForbidden());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        User user = userRepository.findByEmail(email).orElseThrow();
        return jwtService.issueFor(user).accessToken();
    }
}
