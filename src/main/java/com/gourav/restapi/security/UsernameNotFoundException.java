package com.gourav.restapi.security;

/**
 * Thrown when {@link UserDetailsService} cannot find the requested username.
 */
public class UsernameNotFoundException extends RuntimeException {

    public UsernameNotFoundException(String message) {
        super(message);
    }
}
