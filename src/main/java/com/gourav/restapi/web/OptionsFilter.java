package com.gourav.restapi.web;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Answers a plain {@code OPTIONS} probe (one without CORS headers) the way the servlet dispatcher
 * always has: an empty {@code 200} advertising the methods the path supports plus {@code OPTIONS},
 * and an empty {@code Accept-Patch}.
 *
 * <p>It sits after the permit-list check, so this only ever applies to public paths; a protected
 * path has already been rejected as unauthenticated by then.
 */
@Provider
@PreMatching
@Priority(1020)
public class OptionsFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!"OPTIONS".equals(requestContext.getMethod())) {
            return;
        }
        String path = "/" + requestContext.getUriInfo().getPath();
        String allow = AllowHeaders.optionsForPath(path);
        if (allow == null) {
            return;
        }
        requestContext.abortWith(Response.ok()
                .header(HttpHeaders.ALLOW, allow)
                .header("Accept-Patch", "")
                .header(HttpHeaders.CONTENT_LENGTH, "0")
                .build());
    }
}
