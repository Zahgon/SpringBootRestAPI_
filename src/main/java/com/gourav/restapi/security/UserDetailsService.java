package com.gourav.restapi.security;

/**
 * Loads user-specific data. Implemented by
 * {@code com.gourav.restapi.config.services.UserDetailsServiceImpl}.
 */
public interface UserDetailsService {

    UserDetails loadUserByUsername(String username);
}
