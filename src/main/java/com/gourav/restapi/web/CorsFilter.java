package com.gourav.restapi.web;

import com.gourav.restapi.server.InternalHeaders;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Cross-origin handling for the authentication endpoints.
 *
 * <p>Only {@code AuthController} ever opted into CORS ({@code origins = "*"}, {@code maxAge =
 * 3600}); every other resource has no cross-origin configuration at all. A pre-flight aimed at a
 * resource that never opted in is therefore rejected outright rather than silently allowed, and a
 * simple cross-origin request to such a resource is answered without any {@code Access-Control-*}
 * header.
 */
@Provider
@PreMatching
@Priority(500)
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String ORIGIN = "Origin";
    private static final String REQUEST_METHOD = "Access-Control-Request-Method";
    private static final String REQUEST_HEADERS = "Access-Control-Request-Headers";
    private static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String MAX_AGE = "Access-Control-Max-Age";

    private static final String CORS_ALLOWED = "com.gourav.restapi.cors.allowed";
    private static final String MAX_AGE_SECONDS = "3600";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String origin = requestContext.getHeaderString(ORIGIN);
        if (origin == null) {
            return;
        }
        String path = "/" + requestContext.getUriInfo().getPath();
        boolean allowed = isCorsEnabled(path);

        String requestMethod = requestContext.getHeaderString(REQUEST_METHOD);
        if ("OPTIONS".equals(requestContext.getMethod()) && requestMethod != null) {
            requestContext.abortWith(preflight(requestContext, allowed, requestMethod));
            return;
        }
        if (allowed) {
            requestContext.setProperty(CORS_ALLOWED, Boolean.TRUE);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (Boolean.TRUE.equals(requestContext.getProperty(CORS_ALLOWED))) {
            responseContext.getHeaders().putSingle(ALLOW_ORIGIN, "*");
        }
    }

    private static Response preflight(ContainerRequestContext requestContext, boolean allowed, String requestMethod) {
        if (!allowed) {
            return Response.status(Response.Status.FORBIDDEN)
                    .header(InternalHeaders.SUPPRESS_CONTENT_TYPE, "1")
                    .entity("Invalid CORS request")
                    .build();
        }
        Response.ResponseBuilder builder = Response.ok()
                .header(ALLOW_ORIGIN, "*")
                .header(ALLOW_METHODS, requestMethod);
        String requestHeaders = requestContext.getHeaderString(REQUEST_HEADERS);
        if (requestHeaders != null) {
            builder.header(ALLOW_HEADERS, requestHeaders);
        }
        return builder.header(MAX_AGE, MAX_AGE_SECONDS)
                .header(HttpHeaders.CONTENT_LENGTH, "0")
                .build();
    }

    private static boolean isCorsEnabled(String path) {
        return path.startsWith("/api/auth/") || path.equals("/api/auth");
    }
}
