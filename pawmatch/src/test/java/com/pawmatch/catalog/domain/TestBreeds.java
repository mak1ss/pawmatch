package com.pawmatch.catalog.domain;

import java.util.List;
import java.util.UUID;

/** Test fixtures for catalogue tests. Values follow the real Labrador Retriever card from dataset v1.0.0. */
public final class TestBreeds {

    public static final UUID LABRADOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private TestBreeds() {
    }

    public static BreedScores labradorScores() {
        return new BreedScores(5, 2, 4, 5, 5, 5, 4, 3, 5, 5, 5, 5, 2, 3);
    }

    public static Breed labrador() {
        return breed(LABRADOR_ID, "labrador-retriever", null);
    }

    public static Breed labradorWithImage() {
        return breed(LABRADOR_ID, "labrador-retriever", image("labrador-retriever.webp"));
    }

    public static Breed breed(UUID id, String slug, BreedImageMetadata image) {
        return new Breed(id, slug, "Labrador Retriever", "Лабрадор-ретривер", "Sporting Group", Size.LARGE,
                "65-80 pounds (male), 55-70 pounds (female)", "22.5-24.5 inches (male), 21.5-23.5 inches (female)",
                "11-13 years", labradorScores(), List.of("Short"), List.of("Double"),
                null, null, null, null, "https://www.akc.org/dog-breeds/labrador-retriever/", image);
    }

    public static BreedImageMetadata image(String storageKey) {
        return new BreedImageMetadata(storageKey, "image/webp",
                new ImageAttribution("https://commons.wikimedia.org/wiki/File:Labrador.webp", "Jane Doe", "CC BY-SA 4.0"));
    }
}
