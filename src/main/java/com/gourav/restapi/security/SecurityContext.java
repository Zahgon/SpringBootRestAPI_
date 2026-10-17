package com.gourav.restapi.security;

/**
 * Holder for the {@link Authentication} associated with the current request.
 */
public class SecurityContext {

    private Authentication authentication;

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }
}
