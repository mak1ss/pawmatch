package com.pawmatch.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.pawmatch.catalog.application.port.out.BreedImageStorage;
import com.pawmatch.catalog.application.port.out.BreedRepository;
import com.pawmatch.catalog.domain.Breed;
import com.pawmatch.catalog.domain.BreedImage;
import com.pawmatch.catalog.domain.TestBreeds;
import com.pawmatch.common.kernel.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

class BreedCatalogServiceTest {

    private final Map<String, Breed> breeds = new HashMap<>();
    private final Map<String, byte[]> files = new HashMap<>();
    private final BreedRepository repository = new BreedRepository() {
        @Override
        public List<Breed> findAll() {
            return List.copyOf(breeds.values());
        }

        @Override
        public Optional<Breed> findBySlug(String slug) {
            return Optional.ofNullable(breeds.get(slug));
        }

        @Override
        public String currentDatasetVersion() {
            return "1.0.0";
        }
    };
    private final BreedImageStorage storage = key -> Optional.ofNullable(files.get(key));
    private final BreedCatalogService service = new BreedCatalogService(repository, storage);

    @Test
    void listsAndGetsBreeds() {
        breeds.put("labrador-retriever", TestBreeds.labrador());

        assertThat(service.listBreeds()).extracting(Breed::slug).containsExactly("labrador-retriever");
        assertThat(service.getBreed("labrador-retriever").nameEn()).isEqualTo("Labrador Retriever");
    }

    @Test
    void unknownBreedIsNotFound() {
        assertThatThrownBy(() -> service.getBreed("unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("breed not found: unknown");
    }

    @Test
    void returnsImageWithMetadata() {
        breeds.put("labrador-retriever", TestBreeds.labradorWithImage());
        files.put("labrador-retriever.webp", new byte[] {1, 2, 3});

        BreedImage image = service.getBreedImage("labrador-retriever");

        assertThat(image.content()).containsExactly(1, 2, 3);
        assertThat(image.metadata().contentType()).isEqualTo("image/webp");
    }

    @Test
    void breedWithoutImageIsNotFound() {
        breeds.put("labrador-retriever", TestBreeds.labrador());

        assertThatThrownBy(() -> service.getBreedImage("labrador-retriever"))
                .isInstanceOfSatisfying(ResourceNotFoundException.class,
                        e -> assertThat(e.getResource()).isEqualTo("breed_image"));
    }

    @Test
    void missingImageFileIsAServerError() {
        breeds.put("labrador-retriever", TestBreeds.labradorWithImage());

        assertThatIllegalStateException().isThrownBy(() -> service.getBreedImage("labrador-retriever"))
                .withMessageContaining("labrador-retriever.webp");
    }
}
