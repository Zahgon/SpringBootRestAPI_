package com.gourav.restapi.web;

import com.gourav.restapi.config.WebSecurityConfig;
import com.gourav.restapi.config.jwt.AuthEntryPointJwt;
import com.gourav.restapi.security.Authentication;
import com.gourav.restapi.security.SecurityContextHolder;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

/**
 * Enforces the permit list: anything outside it needs an authenticated caller.
 *
 * <p>This runs before routing, exactly like the security chain it replaces, so an anonymous request
 * for an unmapped path is rejected as unauthenticated rather than reported as missing.
 */
@Provider
@PreMatching
@Priority(1010)
public class AuthorizationFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = "/" + requestContext.getUriInfo().getPath();
        if (WebSecurityConfig.isPublic(path)) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            requestContext.abortWith(AuthEntryPointJwt.commence("Full authentication is required to access this resource"));
        }
    }
}
