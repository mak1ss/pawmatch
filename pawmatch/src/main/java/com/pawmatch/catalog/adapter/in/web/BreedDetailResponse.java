package com.pawmatch.catalog.adapter.in.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pawmatch.catalog.domain.Breed;
import com.pawmatch.catalog.domain.BreedScores;
import com.pawmatch.catalog.domain.ImageAttribution;

/**
 * Full breed card. Unknown values are sent as explicit {@code null} (never omitted, never zero), so the client
 * can show "unknown".
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
record BreedDetailResponse(
        UUID id,
        String slug,
        String nameUk,
        String nameEn,
        String akcGroup,
        String size,
        String weight,
        String height,
        String lifeExpectancy,
        Scores scores,
        List<String> coatLength,
        List<String> coatType,
        BigDecimal aloneToleranceHours,
        Integer exerciseMinutesPerDay,
        Integer noviceSuitability,
        Integer goodWithCats,
        String sourceUrl,
        String imageUrl,
        ImageAttributionResponse imageAttribution) {

    static BreedDetailResponse from(Breed breed, String imageUrl) {
        return new BreedDetailResponse(
                breed.id(), breed.slug(), breed.nameUk(), breed.nameEn(), breed.akcGroup(), breed.size().code(),
                breed.weight(), breed.height(), breed.lifeExpectancy(), Scores.from(breed.scores()),
                breed.coatLengths(), breed.coatTypes(), breed.aloneToleranceHours(), breed.exerciseMinutesPerDay(),
                breed.noviceSuitability(), breed.goodWithCats(), breed.sourceUrl(), imageUrl,
                breed.hasImage() ? ImageAttributionResponse.from(breed.image().attribution()) : null);
    }

    /** The 14 AKC ratings, each 1–5. */
    record Scores(
            int energyLevel,
            int coatGroomingFrequency,
            int sheddingLevel,
            int trainabilityLevel,
            int goodWithYoungChildren,
            int goodWithOtherDogs,
            int mentalStimulationNeeds,
            int barkingLevel,
            int adaptabilityLevel,
            int affectionateWithFamily,
            int opennessToStrangers,
            int playfulnessLevel,
            int droolingLevel,
            int watchdogProtectiveNature) {

        static Scores from(BreedScores s) {
            return new Scores(
                    s.energyLevel(), s.coatGroomingFrequency(), s.sheddingLevel(), s.trainabilityLevel(),
                    s.goodWithYoungChildren(), s.goodWithOtherDogs(), s.mentalStimulationNeeds(), s.barkingLevel(),
                    s.adaptabilityLevel(), s.affectionateWithFamily(), s.opennessToStrangers(), s.playfulnessLevel(),
                    s.droolingLevel(), s.watchdogProtectiveNature());
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    record ImageAttributionResponse(String sourceUrl, String author, String license) {

        static ImageAttributionResponse from(ImageAttribution a) {
            return new ImageAttributionResponse(a.sourceUrl(), a.author(), a.license());
        }
    }
}
