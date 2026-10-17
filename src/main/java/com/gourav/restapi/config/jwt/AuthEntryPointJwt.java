package com.gourav.restapi.config.jwt;

import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the response returned whenever a request is rejected before, or instead of, reaching a
 * resource method.
 *
 * <p>The source application delegated this to a servlet {@code sendError(401, ...)}, which — because
 * the rejection already happened during an error dispatch — could not be rendered into a body and
 * was committed as a bare {@code 401} with {@code Content-Length: 0}. That empty response is the
 * single most common outcome of this API and is reproduced here verbatim.</p>
 */
public final class AuthEntryPointJwt {

    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    private AuthEntryPointJwt() {
    }

    public static Response commence(String message) {
        logger.error("Unauthorized error: {}", message);
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }
}
