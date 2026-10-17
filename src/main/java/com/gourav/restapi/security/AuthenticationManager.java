package com.gourav.restapi.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Authenticates a username/password pair against the user store.
 *
 * <p>Equivalent to the DAO authentication provider that used to be assembled
 * in {@code WebSecurityConfig}: look the user up, compare the presented
 * password with the stored hash, and return a fully populated
 * {@link Authentication}. A missing user surfaces as
 * {@link UsernameNotFoundException} and a wrong password as
 * {@link BadCredentialsException}; both leave the request unauthenticated.</p>
 */
@ApplicationScoped
public class AuthenticationManager {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public AuthenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    public Authentication authenticate(Authentication request) {
        String username = String.valueOf(request.getPrincipal());
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        CharSequence presented = request.getCredentials() == null ? "" : String.valueOf(request.getCredentials());
        if (!passwordEncoder.matches(presented, userDetails.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
