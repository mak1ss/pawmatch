package com.pawmatch.catalog.application.port.in;

import java.util.List;

import com.pawmatch.catalog.domain.Breed;

/** Lists the whole breed catalogue, ordered by slug. */
public interface ListBreedsUseCase {

    List<Breed> listBreeds();
}
