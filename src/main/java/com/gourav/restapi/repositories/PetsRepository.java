package com.gourav.restapi.repositories;

import com.gourav.restapi.models.Pets;
import com.gourav.restapi.repositories.paging.Page;
import com.gourav.restapi.repositories.paging.Pageable;

import java.util.Optional;

public interface PetsRepository {

    /** Page through every pet. */
    Page<Pets> findAll(Pageable pageable);

    /** Filter by species (case-insensitive partial match). */
    Page<Pets> findBySpeciesContainingIgnoreCase(String species, Pageable pageable);

    /** Filter by exact adoption status (e.g. AVAILABLE, ADOPTED, PENDING). */
    Page<Pets> findByAdoptionStatus(String adoptionStatus, Pageable pageable);

    /** Filter by both species and adoption status combined. */
    Page<Pets> findBySpeciesContainingIgnoreCaseAndAdoptionStatus(
            String species, String adoptionStatus, Pageable pageable);

    /** Look a pet up by its identifier. */
    Optional<Pets> findById(String id);

    /** Insert or update a pet, returning the stored state. */
    Pets save(Pets pets);

    /** Remove a pet. */
    void delete(Pets pets);

    /** Total number of stored pets. */
    long count();
}
