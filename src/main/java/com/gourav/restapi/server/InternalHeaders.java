package com.gourav.restapi.server;

/**
 * Marker headers used to communicate between the JAX-RS layer and {@link ResponseShapingFilter}.
 *
 * <p>They never reach the wire: the shaping filter consumes and strips them while it is laying the
 * response headers out in their canonical order.
 */
public final class InternalHeaders {

    /** Instructs the shaping filter to emit the response without a {@code Content-Type} header. */
    public static final String SUPPRESS_CONTENT_TYPE = "X-Internal-Suppress-Content-Type";

    private InternalHeaders() {
    }
}
