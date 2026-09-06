package com.servicedesk.ticketing.auth;

/**
 * Thrown when login credentials are invalid, for either an unknown email or
 * an incorrect password. Both cases must be indistinguishable to the caller
 * to avoid leaking whether a given email is registered.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
