package com.gourav.restapi.config;

import com.gourav.restapi.config.jwt.AuthTokenFilter;
import com.gourav.restapi.config.jwt.JwtUtils;
import com.gourav.restapi.config.services.UserDetailsServiceImpl;
import com.gourav.restapi.controllers.ActuatorController;
import com.gourav.restapi.controllers.AuthController;
import com.gourav.restapi.controllers.ErrorController;
import com.gourav.restapi.controllers.GlobalExceptionHandler;
import com.gourav.restapi.controllers.OpenApiController;
import com.gourav.restapi.controllers.PetsController;
import com.gourav.restapi.controllers.SwaggerUiController;
import com.gourav.restapi.repositories.RoleRepository;
import com.gourav.restapi.repositories.UserRepository;
import com.gourav.restapi.security.AuthenticationManager;
import com.gourav.restapi.security.PasswordEncoder;
import com.gourav.restapi.services.PetsService;
import com.gourav.restapi.web.AuthorizationFilter;
import com.gourav.restapi.web.CorsFilter;
import com.gourav.restapi.web.OptionsFilter;
import com.gourav.restapi.web.PathContractFilter;
import com.gourav.restapi.web.RolesFilter;
import com.mongodb.client.MongoDatabase;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/**
 * The JAX-RS application: resources, providers and the request/response pipeline.
 *
 * <p>Resources and the authentication filter are built here from collaborators taken out of the
 * bean container, rather than being looked up themselves: a normal-scoped bean arrives as a proxy
 * subclass, and the annotations the JAX-RS runtime reads off a provider's own class — {@code
 * PreMatching} and {@code Priority} among them — are not inherited by that subclass. Providers
 * without collaborators are registered as classes and built by the JAX-RS runtime.
 */
public class JerseyConfig extends ResourceConfig {

	public JerseyConfig() {
		this(BeanLookup.CDI_CONTAINER);
	}

	public JerseyConfig(BeanLookup beans) {
		register(new AuthController(
				beans.get(AuthenticationManager.class),
				beans.get(UserRepository.class),
				beans.get(RoleRepository.class),
				beans.get(PasswordEncoder.class),
				beans.get(JwtUtils.class)));
		register(new PetsController(beans.get(PetsService.class)));
		register(new ActuatorController(
				beans.get(MongoDatabase.class), beans.get(AppProperties.class)));
		register(new SwaggerUiController());
		register(new ErrorController());
		register(new OpenApiController(beans.get(OpenApiConfig.class)));
		register(new AuthTokenFilter(
				beans.get(JwtUtils.class), beans.get(UserDetailsServiceImpl.class)));

		register(PathContractFilter.class);
		register(CorsFilter.class);
		register(AuthorizationFilter.class);
		register(OptionsFilter.class);
		register(RolesFilter.class);
		register(JacksonConfig.class);
		// An unreadable request body is not a failure the API describes: it collapses into the same
		// unauthorized answer as everything else, so the JSON provider's own mappers stay out.
		register(JacksonFeature.withoutExceptionMappers());
		register(GlobalExceptionHandler.ValidationExceptionMapper.class);
		register(GlobalExceptionHandler.ApiStatusExceptionMapper.class);
		register(GlobalExceptionHandler.RoleNotFoundExceptionMapper.class);
		register(GlobalExceptionHandler.CatchAllExceptionMapper.class);

		// Responses carrying an entity are streamed, as they were before the port.
		property(ServerProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, 0);
		property(ServerProperties.WADL_FEATURE_DISABLE, true);
		// A redirect to the console advertises the path it always advertised, not an absolute URI.
		property(ServerProperties.LOCATION_HEADER_RELATIVE_URI_RESOLUTION_DISABLED, true);
		// An empty error response is written as a plain status: routing it through the container's
		// error page instead would strip Vary, Cache-Control and Expires from the header block.
		property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true);
	}
}
