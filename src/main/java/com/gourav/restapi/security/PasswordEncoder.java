package com.gourav.restapi.security;

/**
 * One-way password hashing contract.
 */
public interface PasswordEncoder {

    String encode(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, String encodedPassword);
}
