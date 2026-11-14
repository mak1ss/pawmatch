package com.pawmatch.catalog.application.port.out;

import java.util.Optional;

/** Loads breed photo bytes by storage key. Where and how they're stored is up to the adapter. */
public interface BreedImageStorage {

    /** The image bytes, or empty if nothing is stored under this key. */
    Optional<byte[]> load(String storageKey);
}
