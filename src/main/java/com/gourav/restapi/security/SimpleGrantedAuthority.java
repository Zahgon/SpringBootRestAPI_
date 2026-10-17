package com.gourav.restapi.security;

import java.util.Objects;

/**
 * Immutable {@link GrantedAuthority} backed by a plain string.
 */
public class SimpleGrantedAuthority implements GrantedAuthority {

    private final String role;

    public SimpleGrantedAuthority(String role) {
        this.role = role;
    }

    @Override
    public String getAuthority() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SimpleGrantedAuthority other)) {
            return false;
        }
        return Objects.equals(role, other.role);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(role);
    }

    @Override
    public String toString() {
        return role;
    }
}
