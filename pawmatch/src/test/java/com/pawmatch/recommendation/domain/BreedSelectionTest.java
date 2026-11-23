package com.pawmatch.recommendation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pawmatch.common.kernel.FieldError;
import com.pawmatch.common.kernel.RequestValidationException;
import org.junit.jupiter.api.Test;

class BreedSelectionTest {

    private static final Set<String> KNOWN = Set.of("beagle", "poodle", "pug", "boxer", "akita", "chihuahua");

    @Test
    void keepsTheRequestedOrder() {
        BreedSelection selection = BreedSelection.parse(Map.of("breed_ids", List.of("pug", "beagle", "akita")), KNOWN);

        assertThat(selection.breedIds()).containsExactly("pug", "beagle", "akita");
    }

    @Test
    void acceptsTwoAndFiveBreeds() {
        assertThat(BreedSelection.parse(Map.of("breed_ids", List.of("pug", "beagle")), KNOWN).breedIds()).hasSize(2);
        assertThat(BreedSelection.parse(Map.of("breed_ids", List.of("pug", "beagle", "akita", "boxer", "poodle")),
                KNOWN).breedIds()).hasSize(5);
    }

    @Test
    void missingBreedIdsIsRequired() {
        assertThat(errors(Map.of())).containsExactly(
                new FieldError("breed_ids", "required", "Обов’язкове поле."));
        assertThat(errors(null)).extracting(FieldError::code).containsExactly("required");
    }

    @Test
    void notAListOfStringsIsInvalidList() {
        assertThat(errors(Map.of("breed_ids", "pug,beagle"))).extracting(FieldError::code)
                .containsExactly("invalid_list");
        assertThat(errors(Map.of("breed_ids", List.of("pug", 3)))).extracting(FieldError::code)
                .containsExactly("invalid_list");
        Map<String, Object> withNull = new HashMap<>();
        withNull.put("breed_ids", Arrays.asList("pug", null));
        assertThat(errors(withNull)).extracting(FieldError::code).containsExactly("invalid_list");
    }

    @Test
    void wrongCountCarriesTheRange() {
        assertThat(errors(Map.of("breed_ids", List.of("pug")))).singleElement().satisfies(error -> {
            assertThat(error.code()).isEqualTo("invalid_count");
            assertThat(error.params()).isEqualTo(Map.of("min", 2, "max", 5));
        });
        assertThat(errors(Map.of("breed_ids", List.of()))).extracting(FieldError::code)
                .containsExactly("invalid_count");
        assertThat(errors(Map.of("breed_ids", List.of("pug", "beagle", "akita", "boxer", "poodle", "chihuahua"))))
                .extracting(FieldError::code).containsExactly("invalid_count");
    }

    @Test
    void collectsEveryProblemAtOnce() {
        List<FieldError> errors = errors(Map.of(
                "breed_ids", List.of("pug", "pug", "unicorn", "pug", "dragon", "unicorn"),
                "zzz", true, "limit", 3));

        assertThat(errors).extracting(FieldError::field, FieldError::code).containsExactly(
                tuple("limit", "unknown_field"),
                tuple("zzz", "unknown_field"),
                tuple("breed_ids", "invalid_count"),
                tuple("breed_ids", "duplicate_value"),
                tuple("breed_ids", "unknown_breed"));
        assertThat(errors.get(3).params()).isEqualTo(Map.of("duplicates", List.of("pug", "unicorn")));
        assertThat(errors.get(4).params()).isEqualTo(Map.of("unknown", List.of("unicorn", "dragon")));
    }

    private static List<FieldError> errors(Map<String, ?> raw) {
        try {
            BreedSelection.parse(raw, KNOWN);
        } catch (RequestValidationException e) {
            return e.getFieldErrors();
        }
        return fail("expected a validation error");
    }

    @Test
    void selectionIsImmutable() {
        BreedSelection selection = BreedSelection.parse(Map.of("breed_ids", List.of("pug", "beagle")), KNOWN);

        assertThatThrownBy(() -> selection.breedIds().add("akita")).isInstanceOf(UnsupportedOperationException.class);
    }
}
