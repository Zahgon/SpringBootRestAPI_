package com.gourav.restapi.config.jwt;

import com.gourav.restapi.config.services.UserDetailsServiceImpl;
import com.gourav.restapi.security.SecurityContextHolder;
import com.gourav.restapi.security.UserDetails;
import com.gourav.restapi.security.UsernamePasswordAuthenticationToken;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the bearer token from the request and, when it is valid, populates the security context.
 *
 * <p>This filter never rejects a request: exactly like the source filter it is best-effort, and a
 * request that carries no token — or a broken one — simply continues as anonymous and is rejected
 * later by {@code AuthorizationFilter} if the path is protected.</p>
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class AuthTokenFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthTokenFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        SecurityContextHolder.clearContext();
        try {
            String jwt = parseJwt(requestContext);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }
    }

    private String parseJwt(ContainerRequestContext requestContext) {
        String headerAuth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (headerAuth != null && !headerAuth.isBlank() && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
