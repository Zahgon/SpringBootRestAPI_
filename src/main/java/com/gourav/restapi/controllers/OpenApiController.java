package com.gourav.restapi.controllers;

import com.gourav.restapi.config.OpenApiConfig;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Serves the OpenAPI document at {@code /v3/api-docs} (JSON) and {@code /v3/api-docs.yaml} (YAML).
 *
 * <p>The document is the one the API has always published: its metadata and security scheme come
 * from {@link OpenApiConfig}, its single server entry is the URL the request arrived on, and its
 * {@code paths} and {@code components.schemas} are the published description of the routes, kept in
 * {@code openapi/api-description.json}. That description is maintained as a resource rather than
 * re-derived from the resource classes, because a scanner reports the JAX-RS view of the routes
 * (tags, response codes, media types, path spelling and ordering) instead of the contract callers
 * already depend on. Both renderings are written whole, with a measured {@code Content-Length}.
 */
@Path("/v3")
public class OpenApiController {

	static final String DESCRIPTION = "openapi/api-description.json";
	static final String SERVER_DESCRIPTION = "Generated server url";
	static final String YAML = "application/vnd.oai.openapi";

	private final OpenApiConfig openApiConfig;

	private volatile OpenAPI description;

	public OpenApiController(OpenApiConfig openApiConfig) {
		this.openApiConfig = openApiConfig;
	}

	@GET
	@Path("api-docs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response json(@Context UriInfo uriInfo) throws IOException {
		return measured(Json.mapper().writeValueAsString(document(uriInfo)), MediaType.APPLICATION_JSON);
	}

	@GET
	@Path("api-docs.yaml")
	@Produces(YAML)
	public Response yaml(@Context UriInfo uriInfo) throws IOException {
		return measured(Yaml.mapper().writeValueAsString(document(uriInfo)), YAML);
	}

	private static Response measured(String text, String mediaType) {
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		return Response.ok(bytes, mediaType).header("Content-Length", bytes.length).build();
	}

	OpenAPI document(UriInfo uriInfo) {
		OpenAPI routes = description();
		OpenAPI document = openApiConfig.customOpenAPI();
		document.servers(List.of(new Server().url(serverUrl(uriInfo)).description(SERVER_DESCRIPTION)));
		document.paths(routes.getPaths());
		document.getComponents().schemas(routes.getComponents().getSchemas());
		return document;
	}

	/** The scheme, host and port the request was addressed to, without a trailing slash. */
	private static String serverUrl(UriInfo uriInfo) {
		String base = uriInfo.getBaseUri().toString();
		return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
	}

	private OpenAPI description() {
		OpenAPI current = description;
		if (current == null) {
			synchronized (this) {
				current = description;
				if (current == null) {
					current = load();
					description = current;
				}
			}
		}
		return current;
	}

	private static OpenAPI load() {
		try (InputStream in = OpenApiController.class.getClassLoader().getResourceAsStream(DESCRIPTION)) {
			if (in == null) {
				throw new IllegalStateException("missing classpath resource " + DESCRIPTION);
			}
			return Json.mapper().readValue(in, OpenAPI.class);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
