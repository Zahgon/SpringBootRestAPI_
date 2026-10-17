package com.gourav.restapi;

import com.gourav.restapi.config.AppProperties;
import com.gourav.restapi.config.jwt.JwtSecretValidator;
import com.gourav.restapi.server.HttpServer;
import com.gourav.restapi.utils.DbSeeder;

import jakarta.enterprise.inject.se.SeContainer;

import org.jboss.weld.environment.se.Weld;

public class RestApiApplication {

	/**
	 * Starts the bean container over this application's packages.
	 *
	 * <p>The package set is named explicitly rather than left to classpath scanning: the classes and
	 * the {@code META-INF/beans.xml} that marks them live in one directory once packaged, but in two
	 * separate directories in a build tree, and only an explicit archive behaves the same either way.
	 * Scanning is switched off for the same reason, otherwise the packaged layout contributes every
	 * class a second time and each injection point becomes ambiguous.
	 */
	public static SeContainer startContainer() {
		return new Weld()
				.disableDiscovery()
				.addPackages(true, RestApiApplication.class)
				.initialize();
	}

	public static void main(String[] args) throws Exception {
		SeContainer container = startContainer();
		HttpServer server = start(container);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(server, container)));
		server.join();
	}

	public static HttpServer start(SeContainer container) throws Exception {
		AppProperties properties = container.select(AppProperties.class).get();
		container.select(JwtSecretValidator.class).get();

		HttpServer server = new HttpServer(properties.getInt("server.port", 8081));
		server.start();
		container.select(DbSeeder.class).get().savePets();
		return server;
	}

	public static void shutdown(HttpServer server, SeContainer container) {
		try {
			server.stop();
		} catch (Exception e) {
			// nothing useful can be done while the JVM is shutting down
		}
		container.close();
	}
}
