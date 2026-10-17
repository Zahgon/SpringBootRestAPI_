package com.gourav.restapi.controllers;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * The container's error page, reached directly.
 *
 * <p>{@code /error} was always mapped, and it is not on the permit list, so an anonymous request is
 * rejected like any other protected path. An authenticated request reaches the page without an
 * error to describe: it answers {@code 500} with the page's own placeholder status {@code 999} and
 * reason {@code None}, and with nothing else in the envelope.
 */
@Path("/error")
public class ErrorController {

	private static final DateTimeFormatter TIMESTAMP =
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx");

	@GET
	public Response get() {
		return noError();
	}

	@POST
	public Response post() {
		return noError();
	}

	@PUT
	public Response put() {
		return noError();
	}

	@DELETE
	public Response delete() {
		return noError();
	}

	@PATCH
	public Response patch() {
		return noError();
	}

	private static Response noError() {
		String body = "{\"timestamp\":\"" + TIMESTAMP.format(OffsetDateTime.now(ZoneOffset.UTC))
				+ "\",\"status\":999,\"error\":\"None\"}";
		return Response.serverError().type(MediaType.APPLICATION_JSON_TYPE).entity(body).build();
	}
}
