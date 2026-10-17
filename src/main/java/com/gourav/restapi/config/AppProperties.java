package com.gourav.restapi.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads {@code application.properties} from the classpath and resolves
 * {@code ${NAME:default}} placeholders against system properties and the
 * environment.
 *
 * <p>Replaces the property-source machinery and {@code @Value} injection the
 * previous framework provided. Placeholder syntax, key names and defaults are
 * unchanged so the documented environment variables keep working.</p>
 */
@ApplicationScoped
public class AppProperties {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}");

    private final Properties properties = new Properties();

    @PostConstruct
    public void load() {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read application.properties", e);
        }
    }

    public String get(String key) {
        return resolve(properties.getProperty(key));
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : value;
    }

    public int getInt(String key, int defaultValue) {
        String value = get(key);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value.trim());
    }

    public long getLong(String key, long defaultValue) {
        String value = get(key);
        return value == null || value.isBlank() ? defaultValue : Long.parseLong(value.trim());
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value == null || value.isBlank() ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    private String resolve(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher matcher = PLACEHOLDER.matcher(raw);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String fallback = matcher.group(2) == null ? "" : matcher.group(2);
            String value = System.getProperty(name);
            if (value == null) {
                value = System.getenv(name);
            }
            if (value == null) {
                value = fallback;
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
