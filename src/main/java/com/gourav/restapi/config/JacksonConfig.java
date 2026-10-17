package com.gourav.restapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * Supplies the JSON mapper. The settings are the ones the API's responses were always serialised
 * with: {@code Instant} rendered as an ISO-8601 string rather than a numeric timestamp.
 */
@Provider
public class JacksonConfig implements ContextResolver<ObjectMapper> {

	private final ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	@Override
	public ObjectMapper getContext(Class<?> type) {
		return objectMapper;
	}
}
