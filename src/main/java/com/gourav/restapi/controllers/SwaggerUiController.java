package com.gourav.restapi.controllers;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Properties;

/**
 * Serves the interactive API console.
 *
 * <p>The console lives at {@code /swagger-ui.html}, which redirects to {@code /swagger-ui/index.html}
 * with a relative {@code Location}; the assets themselves come from the packaged distribution on the
 * classpath. Serving them through the resource layer rather than a static file servlet keeps every
 * request inside the same filter chain, so the permit list still governs them.
 */
@Path("/")
public class SwaggerUiController {

	private static final String WEBJAR_ROOT = "META-INF/resources/webjars/swagger-ui/";

	@GET
	@Path("/swagger-ui.html")
	public Response redirectToConsole() {
		return Response.status(Response.Status.FOUND)
				.location(URI.create("/swagger-ui/index.html"))
				.build();
	}

	@GET
	@Path("/swagger-ui/{asset:.*}")
	public Response asset(@PathParam("asset") String asset) {
		String name = asset.isEmpty() ? "index.html" : asset;
		if (name.contains("..")) {
			throw new NotFoundException();
		}
		URL resource = Thread.currentThread().getContextClassLoader().getResource(root() + name);
		if (resource == null) {
			throw new NotFoundException();
		}
		try {
			URLConnection connection = resource.openConnection();
			long lastModified = connection.getLastModified();
			byte[] bytes;
			try (InputStream stream = connection.getInputStream()) {
				bytes = stream.readAllBytes();
			}
			Response.ResponseBuilder builder = Response.ok(bytes, mediaType(name))
					.header("Accept-Ranges", "bytes")
					.header("Content-Length", bytes.length);
			if (lastModified > 0) {
				builder.lastModified(new Date(lastModified));
			}
			return builder.build();
		} catch (IOException e) {
			throw new NotFoundException();
		}
	}

	/** The packaged console is versioned on the classpath; read the version it ships with. */
	private String root() {
		try (InputStream stream =
				Thread.currentThread()
						.getContextClassLoader()
						.getResourceAsStream("META-INF/maven/org.webjars/swagger-ui/pom.properties")) {
			if (stream != null) {
				Properties properties = new Properties();
				properties.load(stream);
				String version = properties.getProperty("version");
				if (version != null && !version.isBlank()) {
					return WEBJAR_ROOT + version + "/";
				}
			}
		} catch (IOException e) {
			// fall through to the unversioned root
		}
		return WEBJAR_ROOT;
	}

	private String mediaType(String name) {
		if (name.endsWith(".html")) {
			return "text/html";
		}
		if (name.endsWith(".js")) {
			return "application/javascript";
		}
		if (name.endsWith(".css")) {
			return "text/css";
		}
		if (name.endsWith(".png")) {
			return "image/png";
		}
		if (name.endsWith(".json")) {
			return "application/json";
		}
		if (name.endsWith(".map")) {
			return "application/json";
		}
		return "application/octet-stream";
	}
}
