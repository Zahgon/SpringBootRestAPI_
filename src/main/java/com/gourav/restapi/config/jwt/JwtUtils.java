package com.gourav.restapi.config.jwt;

import com.gourav.restapi.config.AppProperties;
import com.gourav.restapi.config.services.UserDetailsImpl;
import com.gourav.restapi.security.Authentication;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    private final AppProperties properties;

    private String jwtSecret;

    private long jwtExpirationMs;

    // Simple in-memory blacklist. Tokens are stored with their expiration time so that
    // entries can be evicted once they would have expired anyway.
    private final Map<String, Long> tokenBlacklist = new ConcurrentHashMap<>();

    @Inject
    public JwtUtils(AppProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        this.jwtSecret = properties.get("app.jwtSecret");
        this.jwtExpirationMs = properties.getLong("app.jwtExpirationMs", 86400000L);
    }

    public String generateJwtToken(Authentication authentication) {

        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            if (isTokenBlacklisted(authToken)) {
                logger.warn("Attempted use of blacklisted token");
                return false;
            }
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Adds a token to the blacklist so that it can no longer be used.
     */
    public void invalidateToken(String token) {
        try {
            Date expiration = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            tokenBlacklist.put(token, expiration.getTime());
            logger.info("Token invalidated. Will be removed from blacklist at expiration time.");
        } catch (JwtException e) {
            logger.warn("Failed to invalidate token: invalid token provided");
        }
    }

    public boolean isTokenBlacklisted(String token) {
        cleanupExpiredTokens();
        return tokenBlacklist.containsKey(token);
    }

    private void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        tokenBlacklist.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
