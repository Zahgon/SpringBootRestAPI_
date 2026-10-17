package com.gourav.restapi.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Document stored in the {@code users} collection.
 *
 * <p>{@code roles} is persisted as an array of database references into the {@code roles}
 * collection; see {@code repositories.mongo.MongoUserRepository}.</p>
 */
@Data
@NoArgsConstructor
public class User {

    private String id;

    @NotBlank
    @Size(max = 20)
    private String username;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    @NotBlank
    @Size(max = 120)
    private String password;

    private Set<Role> roles = new HashSet<>();

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}
