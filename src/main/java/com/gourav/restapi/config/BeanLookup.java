package com.gourav.restapi.config;

import jakarta.enterprise.inject.spi.CDI;

/**
 * Resolves the application's singleton components for the JAX-RS application.
 *
 * <p>In production the components come from the CDI container. Keeping the lookup behind this
 * interface lets the pipeline be assembled against hand-built components as well.
 */
@FunctionalInterface
public interface BeanLookup {

	/** Resolves the single instance of {@code type}. */
	Object lookup(Class<?> type);

	default <T> T get(Class<T> type) {
		return type.cast(lookup(type));
	}

	/** The container-backed lookup used by the running application. */
	BeanLookup CDI_CONTAINER = type -> CDI.current().select(type).get();
}
