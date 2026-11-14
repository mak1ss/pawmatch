package com.pawmatch.catalog.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A breed card from the catalogue.
 * <p>
 * {@code id} is the internal key used for relationships. {@code slug} (e.g. {@code labrador-retriever}) is the stable
 * business key used in URLs, by the scoring model and in anything that leaves the database.
 * <p>
 * Fields documented as nullable mean <b>unknown</b>, never zero: the catalogue has no verified values for them yet.
 *
 * @param weight                free-text weight range from the source; nullable
 * @param height                free-text height range from the source; nullable
 * @param lifeExpectancy        free-text life expectancy from the source; nullable
 * @param coatLengths           coat lengths, e.g. [Short, Medium]; may be empty
 * @param coatTypes             coat types, e.g. [Silky, Wavy]; may be empty
 * @param aloneToleranceHours   hours the breed tolerates being alone; nullable (unknown)
 * @param exerciseMinutesPerDay daily exercise need in minutes; nullable (unknown)
 * @param noviceSuitability     1–5 suitability for first-time owners; nullable (unknown)
 * @param goodWithCats          1–5 rating of living with cats; nullable (unknown)
 * @param sourceUrl             page the breed data comes from (attribution)
 * @param image                 photo metadata; nullable when the breed has no photo yet
 */
public record Breed(
        UUID id,
        String slug,
        String nameEn,
        String nameUk,
        String akcGroup,
        Size size,
        String weight,
        String height,
        String lifeExpectancy,
        BreedScores scores,
        List<String> coatLengths,
        List<String> coatTypes,
        BigDecimal aloneToleranceHours,
        Integer exerciseMinutesPerDay,
        Integer noviceSuitability,
        Integer goodWithCats,
        String sourceUrl,
        BreedImageMetadata image) {

    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final BigDecimal MAX_ALONE_HOURS = BigDecimal.valueOf(24);

    public Breed {
        Objects.requireNonNull(id, "id must not be null");
        if (slug == null || !SLUG.matcher(slug).matches()) {
            throw new IllegalArgumentException("slug must be a lowercase slug, got: '" + slug + "'");
        }
        requireText("name_en", nameEn);
        requireText("name_uk", nameUk);
        requireText("akc_group", akcGroup);
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(scores, "scores must not be null");
        coatLengths = coatLengths == null ? List.of() : List.copyOf(coatLengths);
        coatTypes = coatTypes == null ? List.of() : List.copyOf(coatTypes);
        if (aloneToleranceHours != null
                && (aloneToleranceHours.signum() < 0 || aloneToleranceHours.compareTo(MAX_ALONE_HOURS) > 0)) {
            throw new IllegalArgumentException("alone_tolerance_hours must be between 0 and 24, got: " + aloneToleranceHours);
        }
        if (exerciseMinutesPerDay != null && (exerciseMinutesPerDay < 0 || exerciseMinutesPerDay > 1440)) {
            throw new IllegalArgumentException(
                    "exercise_minutes_per_day must be between 0 and 1440, got: " + exerciseMinutesPerDay);
        }
        BreedScores.requireOptionalScore("novice_suitability", noviceSuitability);
        BreedScores.requireOptionalScore("good_with_cats", goodWithCats);
        requireText("source_url", sourceUrl);
    }

    public boolean hasImage() {
        return image != null;
    }

    static String requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value;
    }
}
