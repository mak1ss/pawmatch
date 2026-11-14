package com.pawmatch.catalog.application.port.in;

import com.pawmatch.catalog.domain.Breed;
import com.pawmatch.common.kernel.ResourceNotFoundException;

/** Returns one breed card by its slug. */
public interface GetBreedUseCase {

    /**
     * @throws ResourceNotFoundException if no breed has this slug
     */
    Breed getBreed(String slug);
}
