package com.gourav.restapi;

import com.gourav.restapi.config.AppProperties;
import com.gourav.restapi.config.JerseyConfig;
import com.gourav.restapi.config.jwt.AuthTokenFilter;
import com.gourav.restapi.config.jwt.JwtUtils;
import com.gourav.restapi.controllers.ActuatorController;
import com.gourav.restapi.controllers.AuthController;
import com.gourav.restapi.controllers.OpenApiController;
import com.gourav.restapi.controllers.PetsController;
import com.gourav.restapi.controllers.SwaggerUiController;
import com.gourav.restapi.server.HttpServer;
import com.gourav.restapi.services.PetsService;
import com.gourav.restapi.support.RawHttp;
import com.gourav.restapi.utils.DbSeeder;
import jakarta.enterprise.inject.se.SeContainer;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RestApiApplicationTest {

	@Test
	public void contextLoads() {
		try (SeContainer container = RestApiApplication.startContainer()) {
			assertNotNull(container.select(AppProperties.class).get());
			assertNotNull(container.select(JwtUtils.class).get());
			assertNotNull(container.select(PetsService.class).get());
			assertNotNull(container.select(DbSeeder.class).get());

			JerseyConfig application = new JerseyConfig(type -> container.select(type).get());
			Set<Class<?>> registered = application.getInstances().stream()
					.map(Object::getClass)
					.collect(Collectors.toSet());
			assertTrue(registered.containsAll(Set.of(
					AuthController.class,
					PetsController.class,
					ActuatorController.class,
					SwaggerUiController.class,
					OpenApiController.class,
					AuthTokenFilter.class)));
		}
	}

	@Test
	public void applicationServesRequestsAndShutsDown() throws Exception {
		System.setProperty("SERVER_PORT", "0");
		System.setProperty(
				"JWT_SECRET", "ci-test-secret-value-minimum-64-chars-required-for-hs512-algorithm");
		SeContainer container = RestApiApplication.startContainer();
		HttpServer server = RestApiApplication.start(container);
		try {
			assertEquals(401, RawHttp.get(server.getPort(), "/pets/").status());
		} finally {
			RestApiApplication.shutdown(server, container);
			System.clearProperty("SERVER_PORT");
			System.clearProperty("JWT_SECRET");
		}
	}
}
