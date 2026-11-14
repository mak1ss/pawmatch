package com.pawmatch.catalog.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.DefaultResourceLoader;

class ResourceBreedImageStorageTest {

    @TempDir
    Path dir;

    @Test
    void loadsExistingFileAndCachesIt() throws IOException {
        Files.write(dir.resolve("beagle.webp"), new byte[] {7, 8, 9});
        ResourceBreedImageStorage storage = storage();

        assertThat(storage.load("beagle.webp")).hasValueSatisfying(bytes -> assertThat(bytes).containsExactly(7, 8, 9));

        Files.delete(dir.resolve("beagle.webp"));
        assertThat(storage.load("beagle.webp")).as("served from cache").isPresent();
    }

    @Test
    void missingFileIsEmpty() {
        assertThat(storage().load("missing.webp")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"../secret.txt", "sub/beagle.webp", "..", ".hidden", "a\\b.webp"})
    void rejectsUnsafeKeys(String key) {
        assertThat(storage().load(key)).isEmpty();
    }

    @Test
    void bundledDefaultLocationIsUsableWhenEmpty() {
        ResourceBreedImageStorage storage =
                new ResourceBreedImageStorage(new DefaultResourceLoader(), "classpath:catalog/images/");

        assertThat(storage.load("no-such-breed.webp")).isEmpty();
    }

    private ResourceBreedImageStorage storage() {
        return new ResourceBreedImageStorage(new DefaultResourceLoader(), dir.toUri().toString());
    }
}
