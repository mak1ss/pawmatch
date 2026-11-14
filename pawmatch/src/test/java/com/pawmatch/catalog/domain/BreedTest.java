package com.pawmatch.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BreedTest {

    @Test
    void validBreedKeepsUnknownValuesAsNull() {
        Breed breed = TestBreeds.labrador();

        assertThat(breed.aloneToleranceHours()).isNull();
        assertThat(breed.exerciseMinutesPerDay()).isNull();
        assertThat(breed.noviceSuitability()).isNull();
        assertThat(breed.goodWithCats()).isNull();
        assertThat(breed.hasImage()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Labrador", "labrador_retriever", "-labrador", "labrador-", "lab--rador", "лабрадор"})
    void rejectsInvalidSlug(String slug) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TestBreeds.breed(TestBreeds.LABRADOR_ID, slug, null))
                .withMessageContaining("slug");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1})
    void rejectsScoresOutsideOneToFive(int score) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BreedScores(score, 2, 4, 5, 5, 5, 4, 3, 5, 5, 5, 5, 2, 3))
                .withMessageContaining("energy_level");
    }

    @Test
    void rejectsOutOfRangeOptionalValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> withOptionals(new BigDecimal("24.5"), null, null, null))
                .withMessageContaining("alone_tolerance_hours");
        assertThatIllegalArgumentException().isThrownBy(() -> withOptionals(null, 1441, null, null))
                .withMessageContaining("exercise_minutes_per_day");
        assertThatIllegalArgumentException().isThrownBy(() -> withOptionals(null, null, 0, null))
                .withMessageContaining("novice_suitability");
        assertThatIllegalArgumentException().isThrownBy(() -> withOptionals(null, null, null, 6))
                .withMessageContaining("good_with_cats");
    }

    @Test
    void acceptsKnownOptionalValues() {
        Breed breed = withOptionals(new BigDecimal("4.0"), 60, 3, 4);

        assertThat(breed.aloneToleranceHours()).isEqualByComparingTo("4");
        assertThat(breed.goodWithCats()).isEqualTo(4);
    }

    @Test
    void coatListsAreImmutableCopies() {
        List<String> lengths = new ArrayList<>(List.of("Short", "Medium"));
        Breed breed = new Breed(TestBreeds.LABRADOR_ID, "chihuahua", "Chihuahua", "Чихуахуа", "Toy Group", Size.XSMALL,
                null, null, null, TestBreeds.labradorScores(), lengths, null, null, null, null, null,
                "https://www.akc.org/dog-breeds/chihuahua/", null);
        lengths.add("Long");

        assertThat(breed.coatLengths()).containsExactly("Short", "Medium");
        assertThat(breed.coatTypes()).isEmpty();
    }

    @Test
    void sizeParsesCatalogueCodesInOrder() {
        assertThat(Size.fromCode("XSmall")).isEqualTo(Size.XSMALL);
        assertThat(Size.fromCode("XLarge").ordinal()).isGreaterThan(Size.fromCode("Large").ordinal());
        assertThatIllegalArgumentException().isThrownBy(() -> Size.fromCode("Huge"));
    }

    private static Breed withOptionals(BigDecimal aloneHours, Integer exercise, Integer novice, Integer cats) {
        return new Breed(TestBreeds.LABRADOR_ID, "labrador-retriever", "Labrador Retriever", "Лабрадор-ретривер",
                "Sporting Group", Size.LARGE, null, null, null, TestBreeds.labradorScores(), List.of(), List.of(),
                aloneHours, exercise, novice, cats, "https://www.akc.org/dog-breeds/labrador-retriever/", null);
    }
}
