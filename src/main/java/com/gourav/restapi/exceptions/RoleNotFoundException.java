package com.gourav.restapi.exceptions;

/**
 * Thrown when a role referenced during signup is missing from the roles collection.
 *
 * <p>The HTTP status (500) is applied by {@code RoleNotFoundExceptionMapper}.</p>
 */
public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(String roleName) {
        super("Role '" + roleName + "' not found in the database. Please seed the roles collection — see README.md for instructions.");
    }
}
