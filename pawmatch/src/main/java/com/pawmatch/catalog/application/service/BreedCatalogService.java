package com.pawmatch.catalog.application.service;

import java.util.List;

import com.pawmatch.catalog.application.port.in.GetBreedImageUseCase;
import com.pawmatch.catalog.application.port.in.GetBreedUseCase;
import com.pawmatch.catalog.application.port.in.ListBreedsUseCase;
import com.pawmatch.catalog.application.port.out.BreedImageStorage;
import com.pawmatch.catalog.application.port.out.BreedRepository;
import com.pawmatch.catalog.domain.Breed;
import com.pawmatch.catalog.domain.BreedImage;
import com.pawmatch.catalog.domain.BreedImageMetadata;
import com.pawmatch.common.kernel.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
class BreedCatalogService implements ListBreedsUseCase, GetBreedUseCase, GetBreedImageUseCase {

    private final BreedRepository breedRepository;
    private final BreedImageStorage imageStorage;

    BreedCatalogService(BreedRepository breedRepository, BreedImageStorage imageStorage) {
        this.breedRepository = breedRepository;
        this.imageStorage = imageStorage;
    }

    @Override
    public List<Breed> listBreeds() {
        return breedRepository.findAll();
    }

    @Override
    public Breed getBreed(String slug) {
        return breedRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("breed", slug));
    }

    @Override
    public BreedImage getBreedImage(String slug) {
        Breed breed = getBreed(slug);
        if (!breed.hasImage()) {
            throw new ResourceNotFoundException("breed_image", slug);
        }
        BreedImageMetadata metadata = breed.image();
        byte[] content = imageStorage.load(metadata.storageKey())
                // Metadata exists but the file doesn't: a deployment error, not a client error.
                .orElseThrow(() -> new IllegalStateException(
                        "Image file missing for breed " + slug + ": " + metadata.storageKey()));
        return new BreedImage(metadata, content);
    }
}
