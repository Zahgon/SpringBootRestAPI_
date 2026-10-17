package com.gourav.restapi.web;

import jakarta.annotation.Priority;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

/**
 * Pins the routing table to the set of paths the application actually maps.
 *
 * <p>The resource classes are mounted at {@code /pets} with the collection methods declared on
 * {@code "/"}, which means the collection is reachable only as {@code /pets/} and an item only as
 * {@code /pets/{id}} without a trailing slash. JAX-RS is more forgiving than that by default: it
 * happily matches {@code /pets} and {@code /pets/{id}/} against the same resource methods. This
 * filter rejects the paths that were never mapped so the routing table stays exactly as narrow as
 * it has always been.
 */
@Provider
@PreMatching
@Priority(1)
public class PathContractFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = "/" + requestContext.getUriInfo().getPath();
        if (!isMapped(path)) {
            throw new NotFoundException();
        }
    }

    private static boolean isMapped(String path) {
        if (path.startsWith("/pets")) {
            return isMappedPetsPath(path);
        }
        if (path.startsWith("/api/auth")) {
            return path.equals("/api/auth/login")
                    || path.equals("/api/auth/signup")
                    || path.equals("/api/auth/logout");
        }
        if (path.startsWith("/actuator")) {
            return path.equals("/actuator/health") || path.equals("/actuator/info");
        }
        return true;
    }

    private static boolean isMappedPetsPath(String path) {
        if (path.equals("/pets/")) {
            return true;
        }
        if (!path.startsWith("/pets/") || path.endsWith("/")) {
            return false;
        }
        return path.indexOf('/', "/pets/".length()) < 0;
    }
}
