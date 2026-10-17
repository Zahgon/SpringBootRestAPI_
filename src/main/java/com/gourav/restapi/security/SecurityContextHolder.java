package com.gourav.restapi.security;

/**
 * Thread-bound {@link SecurityContext} store.
 *
 * <p>The container dispatches one request per thread, so a {@link ThreadLocal}
 * reproduces the previous framework's per-request context semantics. The
 * request filter chain clears it once the response has been written.</p>
 */
public final class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> CONTEXT = ThreadLocal.withInitial(SecurityContext::new);

    private SecurityContextHolder() {
    }

    public static SecurityContext getContext() {
        return CONTEXT.get();
    }

    public static void clearContext() {
        CONTEXT.remove();
    }
}
