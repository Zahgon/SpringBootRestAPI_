package com.gourav.restapi.controllers;

import com.gourav.restapi.config.jwt.AuthEntryPointJwt;
import com.gourav.restapi.controllers.payload.response.ErrorResponse;
import com.gourav.restapi.exceptions.ApiStatusException;
import com.gourav.restapi.exceptions.RoleNotFoundException;
import com.gourav.restapi.web.AllowHeaders;
import jakarta.annotation.Priority;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central error handling for the API.
 *
 * <p>Only three failure kinds carry a JSON body: bean-validation failures, explicitly raised
 * {@link ApiStatusException}s, and {@link RoleNotFoundException}. Everything else — an unmatched
 * route, a wrong method, an unsupported media type, an unparsable body, an authorization denial —
 * is answered with a bare {@code 401} and an empty body, which is what the original application
 * produced through its container error dispatch.
 */
public final class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private GlobalExceptionHandler() {
	}

	private static Response buildResponse(Response.Status status,
			String message,
			String path,
			Map<String, String> fieldErrors) {
		ErrorResponse body = new ErrorResponse(
				status.getStatusCode(),
				status.getReasonPhrase(),
				message,
				path,
				fieldErrors);
		return Response.status(status.getStatusCode())
				.type(MediaType.APPLICATION_JSON)
				.entity(body)
				.build();
	}

	private static String path(UriInfo uriInfo) {
		String rawPath = uriInfo.getRequestUri().getRawPath();
		return rawPath == null || rawPath.isEmpty() ? "/" : rawPath;
	}

	/** Bean-validation failures on a request payload. */
	@Provider
	public static class ValidationExceptionMapper
			implements ExceptionMapper<ConstraintViolationException> {

		@Context
		private UriInfo uriInfo;

		@Override
		public Response toResponse(ConstraintViolationException exception) {
			Map<String, String> fieldErrors = new HashMap<>();
			for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
				String field = null;
				for (jakarta.validation.Path.Node node : violation.getPropertyPath()) {
					field = node.getName();
				}
				String message = violation.getMessage() == null ? "Invalid value" : violation.getMessage();
				fieldErrors.putIfAbsent(field, message);
			}
			return buildResponse(Response.Status.BAD_REQUEST, "Validation failed", path(uriInfo), fieldErrors);
		}
	}

	/** Errors raised deliberately by application code with a chosen status. */
	@Provider
	public static class ApiStatusExceptionMapper implements ExceptionMapper<ApiStatusException> {

		@Context
		private UriInfo uriInfo;

		@Override
		public Response toResponse(ApiStatusException exception) {
			Response.Status status = Response.Status.fromStatusCode(exception.getStatusCode());
			return buildResponse(status, exception.getReason(), path(uriInfo), new HashMap<>());
		}
	}

	/** A role referenced during signup is absent from the database — a seeding problem. */
	@Provider
	public static class RoleNotFoundExceptionMapper implements ExceptionMapper<RoleNotFoundException> {

		@Context
		private UriInfo uriInfo;

		@Override
		public Response toResponse(RoleNotFoundException exception) {
			String path = path(uriInfo);
			logger.error("Role not found at {}: {}", path, exception.getMessage());
			return buildResponse(
					Response.Status.INTERNAL_SERVER_ERROR, exception.getMessage(), path, new HashMap<>());
		}
	}

	/**
	 * Everything the three mappers above do not claim.
	 *
	 * <p>The original application let these escape the handler layer, where the container forwarded
	 * them to its error page; the JWT filter did not run on that dispatch, so the request arrived
	 * anonymous, the error page was not public, and the entry point committed a bare {@code 401}.
	 * The status therefore has nothing to do with the original failure, and this mapper reproduces
	 * that collapse deliberately.
	 */
	@Provider
	@Priority(1)
	public static class CatchAllExceptionMapper implements ExceptionMapper<Throwable> {

		@Context
		private UriInfo uriInfo;

		@Override
		public Response toResponse(Throwable exception) {
			Response.ResponseBuilder builder =
					Response.fromResponse(AuthEntryPointJwt.commence(describe(exception)));
			if (exception instanceof NotAllowedException) {
				String allow = AllowHeaders.forPath(path(uriInfo));
				if (allow != null) {
					builder.header("Allow", allow);
				}
			} else if (exception instanceof NotSupportedException) {
				builder.header("Accept", "application/json, application/*+json");
			}
			return builder.build();
		}

		private String describe(Throwable exception) {
			String message = exception.getMessage();
			return message == null ? exception.getClass().getName() : message;
		}
	}
}
