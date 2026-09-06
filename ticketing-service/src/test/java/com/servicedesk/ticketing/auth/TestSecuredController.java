package com.servicedesk.ticketing.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoint used to prove that method-level role security works
 * end to end (see docs/plans/s2-authentication-role-identity.md). Lives in
 * the test source tree only, so it is picked up by component scanning
 * during tests but never shipped in the production application.
 */
@RestController
class TestSecuredController {

    @GetMapping("/api/test/agent-only")
    @PreAuthorize("hasRole('AGENT')")
    public String agentOnly() {
        return "ok";
    }
}
