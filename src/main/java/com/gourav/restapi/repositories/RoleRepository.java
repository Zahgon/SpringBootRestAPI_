package com.gourav.restapi.repositories;

import com.gourav.restapi.models.ERole;
import com.gourav.restapi.models.Role;

import java.util.Optional;

public interface RoleRepository {

    Optional<Role> findByName(ERole name);
}
