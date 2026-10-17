package com.gourav.restapi.exceptions;

/**
 * Signals that a request should terminate with a specific HTTP status and reason.
 *
 * <p>This is the first-party replacement for the source framework's {@code ResponseStatusException}.
 * It is deliberately a plain {@link RuntimeException} rather than a
 * {@code jakarta.ws.rs.WebApplicationException}: JAX-RS honours the status of a
 * {@code WebApplicationException} directly and would bypass the exception mappers that reproduce
 * this application's error contract.</p>
 */
public class ApiStatusException extends RuntimeException {

    private final int statusCode;
    private final String reason;

    public ApiStatusException(int statusCode, String reason) {
        super(statusCode + " \"" + reason + "\"");
        this.statusCode = statusCode;
        this.reason = reason;
    }

    public ApiStatusException(int statusCode, String reason, Throwable cause) {
        super(statusCode + " \"" + reason + "\"", cause);
        this.statusCode = statusCode;
        this.reason = reason;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReason() {
        return reason;
    }
}
