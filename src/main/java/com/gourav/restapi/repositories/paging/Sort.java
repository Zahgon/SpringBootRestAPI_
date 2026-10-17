package com.gourav.restapi.repositories.paging;

import java.util.Objects;

/**
 * Sort specification for a repository query: a single property and a direction.
 */
public final class Sort {

    private final String property;
    private final boolean ascending;

    private Sort(String property, boolean ascending) {
        this.property = Objects.requireNonNull(property, "property must not be null");
        this.ascending = ascending;
    }

    public static Sort by(String property) {
        return new Sort(property, true);
    }

    public Sort ascending() {
        return new Sort(property, true);
    }

    public Sort descending() {
        return new Sort(property, false);
    }

    public String getProperty() {
        return property;
    }

    public boolean isAscending() {
        return ascending;
    }
}
