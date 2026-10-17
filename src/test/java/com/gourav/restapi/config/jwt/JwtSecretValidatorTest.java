package com.gourav.restapi.config.jwt;

import com.gourav.restapi.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtSecretValidatorTest {

    private static final String DEFAULT_SECRET =
            "change-this-development-secret-change-this-development-secret";

    @Mock
    private AppProperties properties;

    private JwtSecretValidator validator(String secret, String profiles) {
        given(properties.get("app.jwtSecret")).willReturn(secret);
        given(properties.get("app.profiles.active", "")).willReturn(profiles);
        return new JwtSecretValidator(properties);
    }

    @Test
    void anAbsentSecretStopsStartup() {
        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> validator(null, "").validate());

        assertEquals(
                "FATAL: JWT_SECRET environment variable is not set. Set JWT_SECRET before running the application.",
                thrown.getMessage());
    }

    @Test
    void aBlankSecretStopsStartup() {
        assertThrows(IllegalArgumentException.class, () -> validator("   ", "").validate());
    }

    @Test
    void theDevelopmentSecretIsRefusedInProduction() {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class, () -> validator(DEFAULT_SECRET, "prod").validate());

        assertEquals(
                "FATAL: JWT_SECRET is set to the default value in production mode. Set a strong, unique JWT_SECRET before running the application.",
                thrown.getMessage());
    }

    @Test
    void theDevelopmentSecretIsAcceptedOutsideProduction() {
        assertDoesNotThrow(() -> validator(DEFAULT_SECRET, "local").validate());
    }

    @Test
    void aShortSecretStopsStartup() {
        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> validator("too-short", "").validate());

        assertEquals(
                "FATAL: JWT_SECRET must be at least 32 characters for HS512 algorithm. Current length: 9. Set a longer secret.",
                thrown.getMessage());
    }

    @Test
    void aLongEnoughSecretIsAccepted() {
        assertDoesNotThrow(() -> validator(
                        "ci-test-secret-value-minimum-64-chars-required-for-hs512-algorithm", "")
                .validate());
    }
}
