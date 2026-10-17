package com.gourav.restapi.security;

/**
 * Thrown when an authenticated principal lacks a role required by the
 * resource method it just matched.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
