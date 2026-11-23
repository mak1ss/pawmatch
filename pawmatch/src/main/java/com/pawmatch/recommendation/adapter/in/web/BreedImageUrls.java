package com.pawmatch.recommendation.adapter.in.web;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.pawmatch.catalog.application.port.in.ListBreedsUseCase;
import com.pawmatch.catalog.domain.Breed;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Resolves breed slugs to absolute photo URLs for the current request, or null for a breed without a photo. */
final class BreedImageUrls {

    private BreedImageUrls() {
    }

    /** Must be called while handling a request: URLs are built from the current context path. */
    static Function<String, String> resolver(ListBreedsUseCase listBreeds) {
        Set<String> withImage = listBreeds.listBreeds().stream()
                .filter(Breed::hasImage).map(Breed::slug).collect(Collectors.toSet());
        return slug -> withImage.contains(slug)
                ? ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/breeds/{slug}/image").buildAndExpand(slug).toUriString()
                : null;
    }
}
