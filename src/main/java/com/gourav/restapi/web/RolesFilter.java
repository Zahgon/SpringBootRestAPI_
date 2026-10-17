package com.gourav.restapi.web;

import com.gourav.restapi.security.AccessDeniedException;
import com.gourav.restapi.security.Authentication;
import com.gourav.restapi.security.GrantedAuthority;
import com.gourav.restapi.security.SecurityContextHolder;
import jakarta.annotation.Priority;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Applies the {@link RolesAllowed} constraint declared on the matched resource method.
 *
 * <p>Method level authorization deliberately runs <em>after</em> routing, so a request that names a
 * method the resource does not offer is reported by the router first and keeps the {@code Allow}
 * header it has always carried.
 */
@Provider
@Priority(Priorities.AUTHORIZATION)
public class RolesFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Method method = resourceInfo.getResourceMethod();
        if (method == null) {
            return;
        }
        RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
        if (rolesAllowed == null) {
            rolesAllowed = resourceInfo.getResourceClass().getAnnotation(RolesAllowed.class);
        }
        if (rolesAllowed == null) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Access Denied");
        }
        Set<String> granted = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        for (String role : rolesAllowed.value()) {
            if (granted.contains(role)) {
                return;
            }
        }
        throw new AccessDeniedException("Access Denied");
    }
}
