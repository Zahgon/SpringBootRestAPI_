package com.gourav.restapi.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gourav.restapi.config.AppProperties;
import com.gourav.restapi.config.BeanLookup;
import com.gourav.restapi.config.JerseyConfig;
import com.gourav.restapi.config.OpenApiConfig;
import com.gourav.restapi.config.jwt.AuthTokenFilter;
import com.gourav.restapi.config.jwt.JwtUtils;
import com.gourav.restapi.config.services.UserDetailsServiceImpl;
import com.gourav.restapi.models.ERole;
import com.gourav.restapi.models.Pets;
import com.gourav.restapi.models.Role;
import com.gourav.restapi.models.User;
import com.gourav.restapi.repositories.PetsRepository;
import com.gourav.restapi.repositories.RoleRepository;
import com.gourav.restapi.repositories.UserRepository;
import com.gourav.restapi.security.AuthenticationManager;
import com.gourav.restapi.security.BCryptPasswordEncoder;
import com.gourav.restapi.security.PasswordEncoder;
import com.gourav.restapi.server.HttpServer;
import com.gourav.restapi.services.PetsService;
import com.gourav.restapi.support.InMemoryRepositories;
import com.gourav.restapi.support.RawHttp;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Pins the parts of the HTTP surface that the previous framework supplied for free and that this
 * application now owns: the response header block and its order, the single status the container
 * produced for every unrouted or unauthorised request, the CORS rules, the transfer encodings and
 * the shape of the error body.
 *
 * <p>These are not route tests. They exist because the observable contract of the service is made
 * mostly of defaults, and a port that gets every handler right can still change the bytes on the
 * wire. Each test below corresponds to bytes captured from the running service before the port.
 */
class FrameworkSurfaceTest {

	private static final String SECRET = "ci-test-secret-value-minimum-64-chars-required-for-hs512-algorithm";
	private static final String PASSWORD = "Passw0rd!";
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static HttpServer server;
	private static int port;
	private static String userToken;
	private static String moderatorToken;
	private static String adminToken;

	@BeforeAll
	static void startServer() throws Exception {
		System.setProperty("JWT_SECRET", SECRET);

		AppProperties properties = new AppProperties();
		properties.load();

		InMemoryRepositories.PetStore pets = new InMemoryRepositories.PetStore();
		InMemoryRepositories.UserStore users = new InMemoryRepositories.UserStore();
		InMemoryRepositories.RoleStore roles = new InMemoryRepositories.RoleStore();

		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		users.save(user("alice", "alice@example.com", passwordEncoder, roles, ERole.ROLE_USER));
		users.save(user("molly", "molly@example.com", passwordEncoder, roles, ERole.ROLE_MODERATOR));
		users.save(user("adam", "adam@example.com", passwordEncoder, roles, ERole.ROLE_ADMIN));

		pets.save(Pets.builder().name("Spike").species("Dog").breed("Bulldog").age(3).color("Brown").build());
		pets.save(Pets.builder().name("Tom").species("Cat").breed("Regular").age(2).color("Grey").build());
		pets.save(Pets.builder().name("Jerry").species("Mouse").breed("Special").age(1).color("Brown").build());

		JwtUtils jwtUtils = new JwtUtils(properties);
		jwtUtils.init();
		UserDetailsServiceImpl userDetailsService = new UserDetailsServiceImpl(users);
		AuthenticationManager authenticationManager =
				new AuthenticationManager(userDetailsService, passwordEncoder);

		MongoDatabase database = mock(MongoDatabase.class);
		when(database.runCommand(any(Document.class))).thenReturn(new Document("maxWireVersion", 25));

		Map<Class<?>, Object> beans = new HashMap<>();
		beans.put(AppProperties.class, properties);
		beans.put(AuthenticationManager.class, authenticationManager);
		beans.put(UserRepository.class, users);
		beans.put(RoleRepository.class, roles);
		beans.put(PetsRepository.class, pets);
		beans.put(PasswordEncoder.class, passwordEncoder);
		beans.put(JwtUtils.class, jwtUtils);
		beans.put(UserDetailsServiceImpl.class, userDetailsService);
		beans.put(PetsService.class, new PetsService(pets));
		beans.put(OpenApiConfig.class, new OpenApiConfig(properties));
		beans.put(MongoDatabase.class, database);

		BeanLookup lookup = beans::get;
		server = new HttpServer(0, new JerseyConfig(lookup));
		server.start();
		port = server.getPort();

		userToken = login("alice");
		moderatorToken = login("molly");
		adminToken = login("adam");
	}

	@AfterAll
	static void stopServer() throws Exception {
		if (server != null) {
			server.stop();
		}
		System.clearProperty("JWT_SECRET");
	}

	// ---------------------------------------------------------------- header block

	@Test
	void everyRoutedResponseCarriesTheSameHeaderBlockInTheSameOrder() {
		RawHttp.Response response = RawHttp.get(port, "/pets/", bearer(userToken));

		assertEquals(200, response.status());
		assertCanonicalBlock(response);
		assertEquals(List.of("Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"),
				response.headers().get("vary"));
	}

	@Test
	void theHeaderBlockIsPresentOnErrorsToo() {
		assertCanonicalBlock(RawHttp.get(port, "/nope", bearer(userToken)));
		assertCanonicalBlock(RawHttp.get(port, "/pets/", Map.of()));
	}

	@Test
	void noContainerIdentityLeaksIntoResponses() {
		RawHttp.Response response = RawHttp.get(port, "/pets/", bearer(userToken));

		assertFalse(response.has("Server"));
		assertFalse(response.has("X-Powered-By"));
	}

	@Test
	void contentTypeFollowsTheSecurityHeadersAndPrecedesTheTransferHeaders() {
		RawHttp.Response response = RawHttp.get(port, "/pets/", bearer(userToken));
		List<String> names = response.orderedNames();

		assertTrue(names.indexOf("Content-Type") > names.indexOf("X-Frame-Options"));
		assertTrue(names.indexOf("Transfer-Encoding") > names.indexOf("Content-Type"));
		assertEquals("application/json", response.header("Content-Type"));
	}

	// ---------------------------------------------------------------- the 401 collapse

	@Test
	void unknownRoutesAnswerWithAnEmptyUnauthorized() {
		RawHttp.Response response = RawHttp.get(port, "/nope", bearer(userToken));

		assertEquals(401, response.status());
		assertEquals("0", response.header("Content-Length"));
		assertEquals("", response.body());
		assertFalse(response.has("Content-Type"));
	}

	@Test
	void theCollectionIsOnlyMappedWithItsTrailingSlash() {
		RawHttp.Response response = RawHttp.get(port, "/pets", bearer(userToken));

		assertEquals(401, response.status());
		assertEquals("", response.body());
	}

	@Test
	void anUnmappedMethodStillAdvertisesTheMethodsThatAreMapped() {
		RawHttp.Response item = RawHttp.request(port, "PATCH", "/pets/pet-1", bearer(userToken), "{}");
		assertEquals(401, item.status());
		assertEquals("DELETE, PUT, GET", item.header("Allow"));
		assertEquals("", item.body());

		RawHttp.Response collection = RawHttp.request(port, "PUT", "/pets/", bearer(userToken), "{}");
		assertEquals(401, collection.status());
		assertEquals("POST, GET", collection.header("Allow"));
	}

	@Test
	void anUnsupportedRequestBodyStillAdvertisesTheTypesThatAreAccepted() {
		Map<String, String> headers = new HashMap<>(bearer(userToken));
		headers.put("Content-Type", "text/plain");
		RawHttp.Response response = RawHttp.request(port, "POST", "/pets/", headers, "hello");

		assertEquals(401, response.status());
		assertEquals("application/json, application/*+json", response.header("Accept"));
		assertEquals("", response.body());
	}

	@Test
	void aRoleDenialIsIndistinguishableFromNotBeingSignedIn() {
		RawHttp.Response denied = RawHttp.request(port, "DELETE", "/pets/pet-1", bearer(userToken), null);
		RawHttp.Response anonymous = RawHttp.request(port, "DELETE", "/pets/pet-1", Map.of(), null);

		assertEquals(401, denied.status());
		assertEquals("", denied.body());
		assertEquals(anonymous.status(), denied.status());
		assertEquals(anonymous.body(), denied.body());
	}

	@Test
	void unparseableQueryValuesCollapseToTheSameUnauthorized() {
		assertEquals(401, RawHttp.get(port, "/pets/?page=abc", bearer(userToken)).status());
		assertEquals(401, RawHttp.get(port, "/pets/?page=-1", bearer(userToken)).status());
		assertEquals(401, RawHttp.get(port, "/pets/?size=0", bearer(userToken)).status());
	}

	@Test
	void aMalformedBearerTokenLeavesTheRequestAnonymous() {
		RawHttp.Response response = RawHttp.get(port, "/pets/", Map.of("Authorization", "Bearer not-a-token"));

		assertEquals(401, response.status());
		assertEquals("", response.body());
	}

	@Test
	void unmappedManagementPathsAreNotPublic() {
		assertEquals(401, RawHttp.get(port, "/actuator/metrics").status());
		assertEquals(401, RawHttp.get(port, "/actuator").status());
	}

	// ---------------------------------------------------------------- CORS and OPTIONS

	@Test
	void preflightIsAnsweredOnlyForTheAuthenticationRoutes() {
		Map<String, String> preflight = Map.of(
				"Origin", "http://example.com",
				"Access-Control-Request-Method", "POST",
				"Access-Control-Request-Headers", "content-type");
		RawHttp.Response allowed = RawHttp.request(port, "OPTIONS", "/api/auth/login", preflight, null);

		assertEquals(200, allowed.status());
		assertEquals("*", allowed.header("Access-Control-Allow-Origin"));
		assertEquals("POST", allowed.header("Access-Control-Allow-Methods"));
		assertEquals("content-type", allowed.header("Access-Control-Allow-Headers"));
		assertEquals("3600", allowed.header("Access-Control-Max-Age"));
		assertEquals("0", allowed.header("Content-Length"));
	}

	@Test
	void preflightAnywhereElseIsRejectedWithAPlainTextExplanation() {
		Map<String, String> preflight = Map.of(
				"Origin", "http://example.com",
				"Access-Control-Request-Method", "GET");
		RawHttp.Response response = RawHttp.request(port, "OPTIONS", "/pets/", preflight, null);

		assertEquals(403, response.status());
		assertEquals("Invalid CORS request", response.body());
		assertFalse(response.has("Content-Type"));
	}

	@Test
	void aSimpleCrossOriginReadIsOnlyAdvertisedOnTheAuthenticationRoutes() {
		RawHttp.Response pets = RawHttp.get(port, "/pets/",
				Map.of("Origin", "http://example.com", "Authorization", "Bearer " + userToken));
		assertEquals(200, pets.status());
		assertFalse(pets.has("Access-Control-Allow-Origin"));

		RawHttp.Response login = RawHttp.request(port, "POST", "/api/auth/login",
				Map.of("Origin", "http://example.com", "Content-Type", "application/json"),
				"{\"username\":\"alice\",\"password\":\"" + PASSWORD + "\"}");
		assertEquals(200, login.status());
		assertEquals("*", login.header("Access-Control-Allow-Origin"));
	}

	@Test
	void aBareOptionsRequestDescribesAPublicRouteAndIsRefusedOnAProtectedOne() {
		RawHttp.Response publicRoute = RawHttp.request(port, "OPTIONS", "/api/auth/login", Map.of(), null);
		assertEquals(200, publicRoute.status());
		assertEquals("POST,OPTIONS", publicRoute.header("Allow"));
		assertTrue(publicRoute.has("Accept-Patch"));
		assertEquals("", publicRoute.header("Accept-Patch"));
		assertEquals("0", publicRoute.header("Content-Length"));

		RawHttp.Response protectedRoute = RawHttp.request(port, "OPTIONS", "/pets/", Map.of(), null);
		assertEquals(401, protectedRoute.status());
		assertFalse(protectedRoute.has("Allow"));
	}

	// ---------------------------------------------------------------- bodies and framing

	@Test
	void responsesWithAnEntityAreStreamedRatherThanMeasured() {
		RawHttp.Response response = RawHttp.get(port, "/pets/", bearer(userToken));

		assertEquals("chunked", response.header("Transfer-Encoding"));
		assertFalse(response.has("Content-Length"));
	}

	@Test
	void jsonIsWrittenWithoutATrailingNewline() {
		RawHttp.Response response = RawHttp.get(port, "/pets/pet-1", bearer(userToken));

		assertEquals(200, response.status());
		assertTrue(response.body().endsWith("}"));
		assertFalse(response.body().endsWith("\n"));
	}

	@Test
	void jsonCarriesNoDecorativeWhitespaceBetweenItsTokens() {
		RawHttp.Response response = RawHttp.get(port, "/pets/pet-1", bearer(userToken));

		assertEquals(200, response.status());
		assertEquals("", outsideStringLiterals(response.body()).replaceAll("[^\\s]", ""));
	}

	/** Everything a reader would not see: the punctuation between the values, without the values. */
	private static String outsideStringLiterals(String json) {
		return json.replaceAll("\"(\\\\.|[^\"\\\\])*\"", "");
	}

	@Test
	void aDeletedResourceAnswersWithNoBodyAndNoFramingHeaders() {
		RawHttp.Response response = RawHttp.request(port, "DELETE", "/pets/pet-3", bearer(adminToken), null);

		assertEquals(204, response.status());
		assertEquals("", response.body());
		assertFalse(response.has("Content-Type"));
		assertFalse(response.has("Content-Length"));
		assertFalse(response.has("Transfer-Encoding"));
	}

	@Test
	void aRejectedBodyIsReportedWithTheErrorEnvelopeInItsPinnedOrder() throws Exception {
		RawHttp.Response response = RawHttp.request(port, "POST", "/pets/",
				withJson(bearer(userToken)), "{}");

		assertEquals(400, response.status());
		assertEquals("application/json", response.header("Content-Type"));
		JsonNode body = MAPPER.readTree(response.body());
		assertEquals(List.of("timestamp", "status", "error", "message", "path", "fieldErrors"), fieldNames(body));
		assertEquals(400, body.get("status").asInt());
		assertEquals("Bad Request", body.get("error").asText());
		assertEquals("Validation failed", body.get("message").asText());
		assertEquals("/pets/", body.get("path").asText());
		JsonNode fieldErrors = body.get("fieldErrors");
		assertEquals("must not be blank", fieldErrors.get("name").asText());
		assertEquals("must not be blank", fieldErrors.get("species").asText());
		assertEquals("must not be blank", fieldErrors.get("breed").asText());
	}

	@Test
	void aMissingResourceIsReportedWithTheRawRequestPath() throws Exception {
		RawHttp.Response response = RawHttp.get(port, "/pets/pet%2D001", bearer(userToken));

		assertEquals(404, response.status());
		JsonNode body = MAPPER.readTree(response.body());
		assertEquals("Pet not found with id: pet-001", body.get("message").asText());
		assertEquals("/pets/pet%2D001", body.get("path").asText());
	}

	@Test
	void theTokenResponseKeepsItsPinnedKeyOrderAndNames() throws Exception {
		RawHttp.Response response = RawHttp.json(port, "POST", "/api/auth/login",
				"{\"username\":\"molly\",\"password\":\"" + PASSWORD + "\"}");

		assertEquals(200, response.status());
		JsonNode body = MAPPER.readTree(response.body());
		assertEquals(List.of("id", "username", "email", "roles", "accessToken", "tokenType"), fieldNames(body));
		assertEquals("Bearer", body.get("tokenType").asText());
		assertEquals("ROLE_MODERATOR", body.get("roles").get(0).asText());
	}

	@Test
	void aPagedReadKeepsItsEnvelopeKeyOrder() throws Exception {
		RawHttp.Response response = RawHttp.get(port, "/pets/", bearer(userToken));
		JsonNode body = MAPPER.readTree(response.body());

		assertEquals(List.of("content", "page", "size", "totalElements", "totalPages", "last"), fieldNames(body));
		assertEquals(List.of("id", "name", "species", "breed", "age", "color", "adoptionStatus",
				"createdAt", "updatedAt"), fieldNames(body.get("content").get(0)));
	}

	// ---------------------------------------------------------------- management and console

	@Test
	void theHealthAndInfoReportsArePublicAndKeepTheirTrees() throws Exception {
		RawHttp.Response health = RawHttp.get(port, "/actuator/health");
		assertEquals(200, health.status());
		assertEquals("application/vnd.spring-boot.actuator.v3+json", health.header("Content-Type"));
		JsonNode healthBody = MAPPER.readTree(health.body());
		assertEquals("UP", healthBody.get("status").asText());
		assertEquals(List.of("diskSpace", "mongo", "ping"), fieldNames(healthBody.get("components")));
		assertEquals(List.of("total", "free", "threshold", "path", "exists"),
				fieldNames(healthBody.at("/components/diskSpace/details")));

		RawHttp.Response info = RawHttp.get(port, "/actuator/info");
		assertEquals(200, info.status());
		JsonNode infoBody = MAPPER.readTree(info.body());
		assertEquals(List.of("name", "description", "version"), fieldNames(infoBody.get("app")));
		assertNotNull(infoBody.at("/app/version").asText());
	}

	@Test
	void theConsoleRedirectsRelativelyAndTheDocumentIsPublic() {
		RawHttp.Response redirect = RawHttp.get(port, "/swagger-ui.html");
		assertEquals(302, redirect.status());
		assertEquals("/swagger-ui/index.html", redirect.header("Location"));
		assertEquals("0", redirect.header("Content-Length"));

		RawHttp.Response document = RawHttp.get(port, "/v3/api-docs");
		assertEquals(200, document.status());
		assertTrue(document.body().contains("\"openapi\""));
	}

	@Test
	void aMissingAuthorizationHeaderOnLogoutIsAClientError() throws Exception {
		RawHttp.Response response = RawHttp.request(port, "POST", "/api/auth/logout", Map.of(), null);

		assertEquals(400, response.status());
		JsonNode body = MAPPER.readTree(response.body());
		assertEquals("Missing or invalid Authorization header. Expected: Authorization: Bearer <token>",
				body.get("message").asText());
		assertEquals("/api/auth/logout", body.get("path").asText());
	}

	@Test
	void aModeratorMayUpdateButNotDelete() {
		RawHttp.Response update = RawHttp.request(port, "PUT", "/pets/pet-2", withJson(bearer(moderatorToken)),
				"{\"name\":\"Tom\",\"species\":\"Cat\",\"breed\":\"Regular\"}");
		assertEquals(200, update.status());

		RawHttp.Response delete = RawHttp.request(port, "DELETE", "/pets/pet-2", bearer(moderatorToken), null);
		assertEquals(401, delete.status());
		assertEquals("", delete.body());
	}

	// ---------------------------------------------------------------- helpers

	private static void assertCanonicalBlock(RawHttp.Response response) {
		List<String> names = response.orderedNames();
		assertEquals(List.of("Vary", "Vary", "Vary"), names.subList(0, 3));

		int start = names.indexOf("X-Content-Type-Options");
		assertTrue(start >= 3, "security headers must follow the Vary block: " + names);
		assertEquals(List.of("X-Content-Type-Options", "X-XSS-Protection", "Cache-Control", "Pragma",
				"Expires", "X-Frame-Options"), names.subList(start, start + 6));
		assertEquals("nosniff", response.header("X-Content-Type-Options"));
		assertEquals("0", response.header("X-XSS-Protection"));
		assertEquals("no-cache, no-store, max-age=0, must-revalidate", response.header("Cache-Control"));
		assertEquals("no-cache", response.header("Pragma"));
		assertEquals("0", response.header("Expires"));
		assertEquals("DENY", response.header("X-Frame-Options"));
	}

	private static List<String> fieldNames(JsonNode node) {
		List<String> names = new ArrayList<>();
		node.fieldNames().forEachRemaining(names::add);
		return names;
	}

	private static Map<String, String> bearer(String token) {
		return Map.of("Authorization", "Bearer " + token);
	}

	private static Map<String, String> withJson(Map<String, String> headers) {
		Map<String, String> all = new HashMap<>(headers);
		all.put("Content-Type", "application/json");
		return all;
	}

	private static String login(String username) throws Exception {
		RawHttp.Response response = RawHttp.json(port, "POST", "/api/auth/login",
				"{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}");
		if (response.status() != 200) {
			throw new IllegalStateException("login for " + username + " failed: " + response.statusLine());
		}
		return MAPPER.readTree(response.body()).get("accessToken").asText();
	}

	private static User user(String username, String email, PasswordEncoder encoder,
			InMemoryRepositories.RoleStore roles, ERole role) {
		User user = new User(username, email, encoder.encode(PASSWORD));
		Set<Role> granted = new LinkedHashSet<>();
		granted.add(roles.findByName(role).orElseThrow());
		user.setRoles(granted);
		return user;
	}
}
