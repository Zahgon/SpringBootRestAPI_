package com.gourav.restapi.security;

/**
 * A single authority (role) granted to an authenticated principal.
 *
 * <p>First-party replacement for the authority abstraction that the previous
 * security framework supplied. The string form is what {@code JwtResponse}
 * exposes to clients, so it must keep the {@code ROLE_*} spelling.</p>
 */
public interface GrantedAuthority {

    String getAuthority();
}
