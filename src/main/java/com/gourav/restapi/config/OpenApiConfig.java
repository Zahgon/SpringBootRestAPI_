package com.gourav.restapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * OpenAPI metadata: title, version, description, contact, license and the bearer-token
 * security scheme, all driven by {@code openapi.info.*} properties exactly as before.
 */
@ApplicationScoped
public class OpenApiConfig {

    public static final String BEARER_AUTH = "BearerAuth";

    private final AppProperties properties;

    @Inject
    public OpenApiConfig(AppProperties properties) {
        this.properties = properties;
    }

    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(properties.get("openapi.info.title"))
                        .version(properties.get("openapi.info.version"))
                        .description(properties.get("openapi.info.description"))
                        .contact(new Contact()
                                .name(properties.get("openapi.info.contact.name"))
                                .url(properties.get("openapi.info.contact.url")))
                        .license(new License()
                                .name(properties.get("openapi.info.license.name"))
                                .url(properties.get("openapi.info.license.url"))))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT access token (from /api/auth/login)")));
    }
}
