package com.gourav.restapi.web;

/**
 * The {@code Allow} header values this API advertises when a request uses the wrong HTTP method.
 *
 * <p>The values — including their ordering and spacing — are part of the observable contract and are
 * therefore pinned here rather than derived from the routing table, which would order them
 * differently.</p>
 */
public final class AllowHeaders {

    private AllowHeaders() {
    }

    /**
     * @param path request path
     * @return the {@code Allow} value for that path, or {@code null} when the path is not routed
     */
    public static String forPath(String path) {
        if ("/pets/".equals(path)) {
            return "POST, GET";
        }
        if (path.startsWith("/pets/") && path.indexOf('/', 6) < 0 && path.length() > 6) {
            return "DELETE, PUT, GET";
        }
        if ("/api/auth/login".equals(path) || "/api/auth/signup".equals(path) || "/api/auth/logout".equals(path)) {
            return "POST";
        }
        return null;
    }

    /**
     * @param path request path
     * @return the {@code Allow} value used for an {@code OPTIONS} probe, or {@code null}
     */
    public static String optionsForPath(String path) {
        String allow = forPath(path);
        if (allow == null) {
            return null;
        }
        return allow.replace(", ", ",") + ",OPTIONS";
    }
}
