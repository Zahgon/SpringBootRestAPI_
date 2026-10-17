package com.gourav.restapi.controllers;

import com.gourav.restapi.config.AppProperties;
import com.mongodb.client.MongoDatabase;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bson.Document;

/**
 * Health and info endpoints.
 *
 * <p>The original application obtained these from a management add-on that exposed exactly two
 * endpoints, {@code health} and {@code info}; every other management path was unmapped. Both the
 * component breakdown of the health report and the {@code info} tree are reproduced here from the
 * same configuration keys.
 */
@Path("/actuator")
@Produces({ActuatorController.ACTUATOR_JSON, MediaType.APPLICATION_JSON})
public class ActuatorController {

	/** The versioned media type the management endpoints have always answered with. */
	public static final String ACTUATOR_JSON = "application/vnd.spring-boot.actuator.v3+json";

	private static final long DISK_THRESHOLD = 10485760L;

	private final MongoDatabase database;
	private final AppProperties properties;

	public ActuatorController(MongoDatabase database, AppProperties properties) {
		this.database = database;
		this.properties = properties;
	}

	@GET
	@Path("/health")
	public Map<String, Object> health() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", "UP");
		Map<String, Object> components = new LinkedHashMap<>();
		components.put("diskSpace", diskSpace());
		components.put("mongo", mongo());
		components.put("ping", Map.of("status", "UP"));
		body.put("components", components);
		return body;
	}

	@GET
	@Path("/info")
	public Map<String, Object> info() {
		Map<String, Object> app = new LinkedHashMap<>();
		app.put("name", properties.get("info.app.name"));
		app.put("description", properties.get("info.app.description"));
		app.put("version", properties.get("info.app.version"));
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("app", app);
		return body;
	}

	private Map<String, Object> diskSpace() {
		File path = new File(".");
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("total", path.getTotalSpace());
		details.put("free", path.getUsableSpace());
		details.put("threshold", DISK_THRESHOLD);
		details.put("path", path.getAbsolutePath());
		details.put("exists", path.exists());
		Map<String, Object> component = new LinkedHashMap<>();
		component.put("status", path.getUsableSpace() >= DISK_THRESHOLD ? "UP" : "DOWN");
		component.put("details", details);
		return component;
	}

	private Map<String, Object> mongo() {
		Document hello = database.runCommand(new Document("hello", 1));
		Map<String, Object> component = new LinkedHashMap<>();
		component.put("status", "UP");
		component.put("details", Map.of("maxWireVersion", hello.getInteger("maxWireVersion")));
		return component;
	}
}
