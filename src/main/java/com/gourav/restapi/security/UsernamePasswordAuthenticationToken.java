package com.gourav.restapi.security;

import java.util.Collection;

/**
 * {@link Authentication} carrying a username and password.
 *
 * <p>The two-argument form is an unauthenticated request handed to
 * {@link AuthenticationManager}; the three-argument form is the authenticated
 * result stored in {@link SecurityContextHolder}.</p>
 */
public class UsernamePasswordAuthenticationToken extends Authentication {

    public UsernamePasswordAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials, null, false);
    }

    public UsernamePasswordAuthenticationToken(Object principal,
                                               Object credentials,
                                               Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities, true);
    }
}
