package com.pawmatch.catalog.adapter.out.persistence;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.pawmatch.catalog.application.port.out.BreedRepository;
import com.pawmatch.catalog.domain.Breed;
import com.pawmatch.catalog.domain.BreedImageMetadata;
import com.pawmatch.catalog.domain.BreedScores;
import com.pawmatch.catalog.domain.ImageAttribution;
import com.pawmatch.catalog.domain.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * {@link BreedRepository} backed by the {@code breed} and {@code breed_image} tables.
 * <p>
 * The catalogue only changes through migrations (i.e. on deploy), so it's loaded once, on first access,
 * and served from memory afterwards. Mapping to the domain also validates every row: a bad row fails loudly.
 */
@Slf4j
@Component
class BreedPersistenceAdapter implements BreedRepository {

    private final BreedJpaRepository breedJpaRepository;
    private final BreedImageJpaRepository imageJpaRepository;
    private final CatalogueDatasetJpaRepository datasetJpaRepository;

    private volatile Map<String, Breed> breedsBySlug;
    private volatile String datasetVersion;

    BreedPersistenceAdapter(BreedJpaRepository breedJpaRepository, BreedImageJpaRepository imageJpaRepository,
                            CatalogueDatasetJpaRepository datasetJpaRepository) {
        this.breedJpaRepository = breedJpaRepository;
        this.imageJpaRepository = imageJpaRepository;
        this.datasetJpaRepository = datasetJpaRepository;
    }

    @Override
    public List<Breed> findAll() {
        return List.copyOf(snapshot().values());
    }

    @Override
    public Optional<Breed> findBySlug(String slug) {
        return Optional.ofNullable(snapshot().get(slug));
    }

    /** Cached like the breeds: the dataset version only changes through migrations. */
    @Override
    public String currentDatasetVersion() {
        String current = datasetVersion;
        if (current == null) {
            current = datasetJpaRepository.findFirstByOrderByLoadedAtDescVersionDesc()
                    .map(CatalogueDatasetJpaEntity::getVersion)
                    .orElseThrow(() -> new IllegalStateException("No breed dataset loaded (catalogue_dataset is empty)"));
            datasetVersion = current;
        }
        return current;
    }

    private Map<String, Breed> snapshot() {
        Map<String, Breed> current = breedsBySlug;
        if (current == null) {
            synchronized (this) {
                current = breedsBySlug;
                if (current == null) {
                    current = load();
                    breedsBySlug = current;
                }
            }
        }
        return current;
    }

    private Map<String, Breed> load() {
        Map<UUID, BreedImageJpaEntity> images = imageJpaRepository.findAll().stream()
                .collect(Collectors.toMap(BreedImageJpaEntity::getBreedId, Function.identity()));
        Map<String, Breed> breeds = breedJpaRepository.findAll().stream()
                .map(entity -> toDomain(entity, images.get(entity.getId())))
                .sorted(Comparator.comparing(Breed::slug))
                .collect(Collectors.toMap(Breed::slug, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        log.info("Breed catalogue loaded: {} breeds, {} with images", breeds.size(), images.size());
        return breeds;
    }

    static Breed toDomain(BreedJpaEntity e, BreedImageJpaEntity image) {
        try {
            return new Breed(
                    e.getId(), e.getSlug(), e.getNameEn(), e.getNameUk(), e.getAkcGroup(), Size.fromCode(e.getSize()),
                    e.getWeight(), e.getHeight(), e.getLifeExpectancy(),
                    new BreedScores(
                            e.getEnergyLevel(), e.getCoatGroomingFrequency(), e.getSheddingLevel(),
                            e.getTrainabilityLevel(), e.getGoodWithYoungChildren(), e.getGoodWithOtherDogs(),
                            e.getMentalStimulationNeeds(), e.getBarkingLevel(), e.getAdaptabilityLevel(),
                            e.getAffectionateWithFamily(), e.getOpennessToStrangers(), e.getPlayfulnessLevel(),
                            e.getDroolingLevel(), e.getWatchdogProtectiveNature()),
                    splitMultiValue(e.getCoatLength()), splitMultiValue(e.getCoatType()),
                    e.getAloneToleranceHours(), e.getExerciseMinutesPerDay(), e.getNoviceSuitability(),
                    e.getGoodWithCats(), e.getSourceUrl(),
                    image == null ? null : new BreedImageMetadata(image.getStorageKey(), image.getContentType(),
                            new ImageAttribution(image.getSourceUrl(), image.getAuthor(), image.getLicense())));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid breed row '" + e.getSlug() + "': " + ex.getMessage(), ex);
        }
    }

    static List<String> splitMultiValue(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\|")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }
}
