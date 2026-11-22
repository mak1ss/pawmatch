package com.pawmatch.catalog.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import com.pawmatch.TestcontainersConfiguration;
import com.pawmatch.catalog.domain.Breed;
import com.pawmatch.catalog.domain.Size;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Runs the real Liquibase migrations against PostgreSQL: schema, seed data, constraints and domain mapping.
 */
@Tag("docker")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class BreedPersistenceAdapterTest {

    @Autowired
    BreedJpaRepository breedJpaRepository;
    @Autowired
    BreedImageJpaRepository imageJpaRepository;
    @Autowired
    CatalogueDatasetJpaRepository datasetJpaRepository;
    @Autowired
    JdbcTemplate jdbc;

    private BreedPersistenceAdapter adapter() {
        return new BreedPersistenceAdapter(breedJpaRepository, imageJpaRepository, datasetJpaRepository);
    }

    @Test
    void seedLoadsAllThirtyBreedsWithGeneratedIds() {
        List<Breed> breeds = adapter().findAll();

        assertThat(breeds).hasSize(30);
        assertThat(breeds).extracting(Breed::id).doesNotContainNull().doesNotHaveDuplicates();
        assertThat(breeds).extracting(Breed::slug).isSorted();
        assertThat(adapter().currentDatasetVersion()).isEqualTo("1.0.0");
    }

    @Test
    void mapsSeededRowToDomain() {
        Breed labrador = adapter().findBySlug("labrador-retriever").orElseThrow();

        assertThat(labrador.nameUk()).isEqualTo("Лабрадор-ретривер");
        assertThat(labrador.size()).isEqualTo(Size.LARGE);
        assertThat(labrador.weight()).isEqualTo("65-80 pounds (male), 55-70 pounds (female)");
        assertThat(labrador.scores().energyLevel()).isEqualTo(5);
        assertThat(labrador.scores().watchdogProtectiveNature()).isEqualTo(3);
        assertThat(labrador.sourceUrl()).isEqualTo("https://www.akc.org/dog-breeds/labrador-retriever/");
        assertThat(labrador.aloneToleranceHours()).isNull();
        assertThat(labrador.exerciseMinutesPerDay()).isNull();
        assertThat(labrador.noviceSuitability()).isNull();
        assertThat(labrador.goodWithCats()).isNull();
        assertThat(labrador.hasImage()).isFalse();
    }

    @Test
    void splitsMultiValueCoatFields() {
        assertThat(adapter().findBySlug("chihuahua").orElseThrow().coatLengths()).containsExactly("Short", "Medium");
        assertThat(adapter().findBySlug("cavalier-king-charles-spaniel").orElseThrow().coatTypes())
                .containsExactly("Silky", "Wavy");
    }

    @Test
    void unknownSlugIsEmpty() {
        assertThat(adapter().findBySlug("no-such-breed")).isEmpty();
    }

    @Test
    void mapsImageMetadataWhenPresent() {
        UUID beagleId = jdbc.queryForObject("SELECT id FROM breed WHERE slug = 'beagle'", UUID.class);
        jdbc.update("""
                INSERT INTO breed_image (breed_id, storage_key, content_type, source_url, author, license)
                VALUES (?, 'beagle.webp', 'image/webp', 'https://commons.wikimedia.org/wiki/File:Beagle.webp',
                        'Jane Doe', 'CC BY-SA 4.0')""", beagleId);

        Breed beagle = adapter().findBySlug("beagle").orElseThrow();

        assertThat(beagle.image().storageKey()).isEqualTo("beagle.webp");
        assertThat(beagle.image().attribution().license()).isEqualTo("CC BY-SA 4.0");
    }

    @Test
    void databaseRejectsOutOfRangeScore() {
        assertThatThrownBy(() -> jdbc.update("UPDATE breed SET energy_level = 6 WHERE slug = 'beagle'"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_breed_energy_level");
    }

    @Test
    void databaseRejectsUnknownSize() {
        assertThatThrownBy(() -> jdbc.update("UPDATE breed SET size = 'Huge' WHERE slug = 'beagle'"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_breed_size");
    }

    @Test
    void databaseRejectsDuplicateSlug() {
        assertThatThrownBy(() -> jdbc.update("UPDATE breed SET slug = 'beagle' WHERE slug = 'boxer'"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_breed_slug");
    }
}
