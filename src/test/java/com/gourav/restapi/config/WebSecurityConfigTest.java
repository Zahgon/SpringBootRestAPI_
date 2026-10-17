package com.gourav.restapi.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSecurityConfigTest {

    @Test
    void authenticationRoutesAndEverythingBelowThemAreReachableWithoutASignIn() {
        assertTrue(WebSecurityConfig.isPublic("/api/auth"));
        assertTrue(WebSecurityConfig.isPublic("/api/auth/login"));
        assertTrue(WebSecurityConfig.isPublic("/api/auth/signup"));
        assertTrue(WebSecurityConfig.isPublic("/api/auth/logout"));
    }

    @Test
    void theConsoleAndItsAssetsAreReachableWithoutASignIn() {
        assertTrue(WebSecurityConfig.isPublic("/swagger-ui.html"));
        assertTrue(WebSecurityConfig.isPublic("/swagger-ui"));
        assertTrue(WebSecurityConfig.isPublic("/swagger-ui/index.html"));
    }

    @Test
    void theDocumentIsReachableWithoutASignIn() {
        assertTrue(WebSecurityConfig.isPublic("/v3/api-docs"));
        assertTrue(WebSecurityConfig.isPublic("/v3/api-docs/swagger-config"));
        assertTrue(WebSecurityConfig.isPublic("/v3/api-docs.yaml"));
    }

    @Test
    void onlyTwoManagementReportsAreReachableWithoutASignIn() {
        assertTrue(WebSecurityConfig.isPublic("/actuator/health"));
        assertTrue(WebSecurityConfig.isPublic("/actuator/info"));
        assertFalse(WebSecurityConfig.isPublic("/actuator"));
        assertFalse(WebSecurityConfig.isPublic("/actuator/metrics"));
        assertFalse(WebSecurityConfig.isPublic("/actuator/health/db"));
    }

    @Test
    void everythingElseNeedsASignIn() {
        assertFalse(WebSecurityConfig.isPublic("/"));
        assertFalse(WebSecurityConfig.isPublic("/pets/"));
        assertFalse(WebSecurityConfig.isPublic("/pets/pet-1"));
        assertFalse(WebSecurityConfig.isPublic("/api/authx/login"));
        assertFalse(WebSecurityConfig.isPublic("/swagger-ui.htmlx"));
        assertFalse(WebSecurityConfig.isPublic("/v3/api-docs.yamlx"));
    }
}
