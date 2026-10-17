package com.gourav.restapi.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * BCrypt {@link PasswordEncoder}.
 *
 * <p>Strength 10 and the {@code $2a$} prefix are kept so that password hashes
 * written by earlier releases of this application keep verifying.</p>
 */
@ApplicationScoped
public class BCryptPasswordEncoder implements PasswordEncoder {

    private static final int STRENGTH = 10;

    @Override
    public String encode(CharSequence rawPassword) {
        return BCrypt.with(BCrypt.Version.VERSION_2A)
                .hashToString(STRENGTH, rawPassword.toString().toCharArray());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }
        return BCrypt.verifyer(BCrypt.Version.VERSION_2A)
                .verify(rawPassword.toString().toCharArray(), encodedPassword)
                .verified;
    }
}
