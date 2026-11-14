package com.pawmatch.catalog.adapter.in.web;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pawmatch.catalog.domain.Breed;

/** Breed card in the catalogue list. {@code image_url} is {@code null} when the breed has no photo. */
@JsonInclude(JsonInclude.Include.ALWAYS)
record BreedSummaryResponse(
        UUID id,
        String slug,
        String nameUk,
        String nameEn,
        String akcGroup,
        String size,
        int energyLevel,
        int sheddingLevel,
        int coatGroomingFrequency,
        int trainabilityLevel,
        int goodWithYoungChildren,
        String imageUrl) {

    static BreedSummaryResponse from(Breed breed, String imageUrl) {
        var scores = breed.scores();
        return new BreedSummaryResponse(
                breed.id(), breed.slug(), breed.nameUk(), breed.nameEn(), breed.akcGroup(), breed.size().code(),
                scores.energyLevel(), scores.sheddingLevel(), scores.coatGroomingFrequency(),
                scores.trainabilityLevel(), scores.goodWithYoungChildren(), imageUrl);
    }
}
