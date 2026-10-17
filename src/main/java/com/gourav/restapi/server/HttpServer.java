package com.gourav.restapi.server;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.EnumSet;

import com.gourav.restapi.config.JerseyConfig;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

/** Builds the embedded servlet container that hosts the JAX-RS application. */
public final class HttpServer {

	private final Tomcat tomcat;

	public HttpServer(int port) {
		this(port, new JerseyConfig());
	}

	public HttpServer(int port, ResourceConfig application) {
		File base = scratchDirectory();

		this.tomcat = new Tomcat();
		tomcat.setBaseDir(base.getAbsolutePath());
		tomcat.setPort(port);
		tomcat.getConnector();
		tomcat.setSilent(true);

		/*
		 * The generic status page is the whole error body the API ever returns from the container:
		 * neither the container's identity nor a stack report belongs in it.
		 */
		ErrorReportValve errors = new ErrorReportValve();
		errors.setShowServerInfo(false);
		errors.setShowReport(false);
		tomcat.getHost().getPipeline().addValve(errors);

		Context context = tomcat.addContext("", base.getAbsolutePath());

		addFilter(context, "rejectedPath", new RejectedPathFilter());

		addFilter(context, "responseShaping", new ResponseShapingFilter());

		Tomcat.addServlet(context, "jersey", new ServletContainer(application)).setLoadOnStartup(1);
		context.addServletMappingDecoded("/*", "jersey");
	}

	private static void addFilter(Context context, String name, Filter filter) {
		FilterDef definition = new FilterDef();
		definition.setFilterName(name);
		definition.setFilter(filter);
		context.addFilterDef(definition);

		FilterMap mapping = new FilterMap();
		mapping.setFilterName(name);
		mapping.addURLPattern("/*");
		EnumSet.allOf(DispatcherType.class).forEach(type -> mapping.setDispatcher(type.name()));
		context.addFilterMap(mapping);
	}

	private static File scratchDirectory() {
		try {
			File directory = Files.createTempDirectory("restapi-").toFile();
			directory.deleteOnExit();
			return directory;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void start() throws Exception {
		tomcat.start();
	}

	public void join() {
		tomcat.getServer().await();
	}

	public void stop() throws Exception {
		tomcat.stop();
		tomcat.destroy();
	}

	public int getPort() {
		return tomcat.getConnector().getLocalPort();
	}
}
