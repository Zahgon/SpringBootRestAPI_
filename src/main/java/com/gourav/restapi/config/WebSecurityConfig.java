package com.gourav.restapi.config;

import java.util.List;

/**
 * The security rules of the application, expressed as a plain path matcher.
 *
 * <p>Under the source framework these rules lived in an {@code authorizeHttpRequests(...)} DSL.
 * The list below is that permit list, in the same order; every other path requires an
 * authenticated request.</p>
 *
 * <p>A trailing {@code /**} matches the prefix and anything below it; every other entry is an
 * exact path.</p>
 */
public final class WebSecurityConfig {

    private static final List<String> PERMIT_ALL = List.of(
            "/api/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/actuator/health",
            "/actuator/info");

    private WebSecurityConfig() {
    }

    /**
     * @param path request path, always starting with {@code /}
     * @return {@code true} when the path is reachable without authentication
     */
    public static boolean isPublic(String path) {
        for (String pattern : PERMIT_ALL) {
            if (matches(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String pattern, String path) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }
        return pattern.equals(path);
    }
}
