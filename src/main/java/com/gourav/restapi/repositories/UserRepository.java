package com.gourav.restapi.repositories;

import com.gourav.restapi.models.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    User save(User user);
}
