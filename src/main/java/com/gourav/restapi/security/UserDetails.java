package com.gourav.restapi.security;

import java.util.Collection;

/**
 * Core user information retrieved by {@link UserDetailsService}.
 */
public interface UserDetails {

    Collection<? extends GrantedAuthority> getAuthorities();

    String getPassword();

    String getUsername();

    boolean isAccountNonExpired();

    boolean isAccountNonLocked();

    boolean isCredentialsNonExpired();

    boolean isEnabled();
}
