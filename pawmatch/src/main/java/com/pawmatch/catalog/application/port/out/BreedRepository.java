package com.pawmatch.catalog.application.port.out;

import java.util.List;
import java.util.Optional;

import com.pawmatch.catalog.domain.Breed;

/** Read access to the breed catalogue, including each breed's photo metadata. */
public interface BreedRepository {

    /** All breeds, ordered by slug. */
    List<Breed> findAll();

    Optional<Breed> findBySlug(String slug);
}
