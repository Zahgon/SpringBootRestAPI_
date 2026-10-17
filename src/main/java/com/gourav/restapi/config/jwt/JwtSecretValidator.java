package com.gourav.restapi.config.jwt;

import com.gourav.restapi.config.AppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Fails fast at startup when the configured JWT secret is unusable.
 */
@ApplicationScoped
public class JwtSecretValidator {

    private static final String DEFAULT_SECRET = "change-this-development-secret-change-this-development-secret";
    private static final int MIN_SECRET_LENGTH = 32;

    private final AppProperties properties;

    @Inject
    public JwtSecretValidator(AppProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void validate() {
        String jwtSecret = properties.get("app.jwtSecret");
        String activeProfiles = properties.get("app.profiles.active", "");

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "FATAL: JWT_SECRET environment variable is not set. Set JWT_SECRET before running the application.");
        }

        if (DEFAULT_SECRET.equals(jwtSecret)) {
            if (activeProfiles.contains("prod")) {
                throw new IllegalArgumentException(
                        "FATAL: JWT_SECRET is set to the default value in production mode. Set a strong, unique JWT_SECRET before running the application.");
            }
            System.err.println("⚠️  WARNING: Using default JWT secret. This is only acceptable for development.");
        }

        if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException(String.format(
                    "FATAL: JWT_SECRET must be at least %d characters for HS512 algorithm. Current length: %d. Set a longer secret.",
                    MIN_SECRET_LENGTH, jwtSecret.length()));
        }
    }
}
