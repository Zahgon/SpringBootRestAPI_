package com.gourav.restapi.security;

import java.util.Collection;
import java.util.List;

/**
 * A request for, or the result of, authenticating a principal.
 */
public class Authentication {

    private final Object principal;
    private final Object credentials;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean authenticated;
    private Object details;

    protected Authentication(Object principal,
                             Object credentials,
                             Collection<? extends GrantedAuthority> authorities,
                             boolean authenticated) {
        this.principal = principal;
        this.credentials = credentials;
        this.authorities = authorities == null ? List.of() : List.copyOf(authorities);
        this.authenticated = authenticated;
    }

    public Object getPrincipal() {
        return principal;
    }

    public Object getCredentials() {
        return credentials;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }

    public String getName() {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return String.valueOf(principal);
    }
}
