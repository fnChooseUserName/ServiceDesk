package com.servicedesk.ticketing.auth;

import com.servicedesk.ticketing.auth.dto.AuthenticatedUserResponse;
import com.servicedesk.ticketing.auth.dto.LoginRequest;
import com.servicedesk.ticketing.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @GetMapping("/api/auth/me")
    public AuthenticatedUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new AuthenticatedUserResponse(
                jwtService.userId(jwt),
                jwtService.fullName(jwt),
                jwtService.email(jwt),
                jwtService.role(jwt)
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}
