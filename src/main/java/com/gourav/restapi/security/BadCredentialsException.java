package com.gourav.restapi.security;

/**
 * Thrown when the presented credentials do not match the stored ones.
 */
public class BadCredentialsException extends RuntimeException {

    public BadCredentialsException(String message) {
        super(message);
    }
}
