package com.pawmatch.catalog.domain;

import static com.pawmatch.catalog.domain.Breed.requireText;

import java.util.Objects;
import java.util.Set;

/**
 * Describes a breed photo without its bytes, so lists and cards can reference it cheaply.
 *
 * @param storageKey  where the image storage finds the bytes, e.g. {@code labrador-retriever.webp}
 * @param contentType MIME type: {@code image/webp}, {@code image/jpeg} or {@code image/png}
 * @param attribution source, author and licence
 */
public record BreedImageMetadata(String storageKey, String contentType, ImageAttribution attribution) {

    public static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of("image/webp", "image/jpeg", "image/png");

    public BreedImageMetadata {
        requireText("storage_key", storageKey);
        if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported image content type: " + contentType);
        }
        Objects.requireNonNull(attribution, "attribution must not be null");
    }
}
