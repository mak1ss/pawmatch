package com.pawmatch.catalog.adapter.in.web;

import java.time.Duration;
import java.util.List;

import com.pawmatch.catalog.application.port.in.GetBreedImageUseCase;
import com.pawmatch.catalog.application.port.in.GetBreedUseCase;
import com.pawmatch.catalog.application.port.in.ListBreedsUseCase;
import com.pawmatch.catalog.domain.Breed;
import com.pawmatch.catalog.domain.BreedImage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Breeds", description = "Breed catalogue: cards, characteristics and photos")
@RestController
@RequestMapping("/api/v1/breeds")
class BreedController {

    private static final CacheControl IMAGE_CACHE = CacheControl.maxAge(Duration.ofDays(1)).cachePublic();

    private final ListBreedsUseCase listBreeds;
    private final GetBreedUseCase getBreed;
    private final GetBreedImageUseCase getBreedImage;

    BreedController(ListBreedsUseCase listBreeds, GetBreedUseCase getBreed, GetBreedImageUseCase getBreedImage) {
        this.listBreeds = listBreeds;
        this.getBreed = getBreed;
        this.getBreedImage = getBreedImage;
    }

    @Operation(summary = "List all breeds", description = "Fields needed for breed cards, ordered by slug.")
    @GetMapping
    List<BreedSummaryResponse> list() {
        return listBreeds.listBreeds().stream()
                .map(breed -> BreedSummaryResponse.from(breed, imageUrl(breed)))
                .toList();
    }

    @Operation(summary = "Get a breed card", description = "All characteristics, data source and photo attribution.")
    @GetMapping("/{slug}")
    BreedDetailResponse get(@PathVariable String slug) {
        Breed breed = getBreed.getBreed(slug);
        return BreedDetailResponse.from(breed, imageUrl(breed));
    }

    /**
     * Returns the photo with an ETag (its SHA-256). Spring answers 304 Not Modified when the client's
     * {@code If-None-Match} matches.
     */
    @Operation(summary = "Get a breed photo", description = "Binary image. 404 if the breed or its photo doesn't exist.")
    @GetMapping("/{slug}/image")
    ResponseEntity<byte[]> image(@PathVariable String slug) {
        BreedImage image = getBreedImage.getBreedImage(slug);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.metadata().contentType()))
                .eTag(image.contentHash())
                .cacheControl(IMAGE_CACHE)
                .body(image.content());
    }

    /** Absolute URL, because the frontend runs on a different origin. */
    private static String imageUrl(Breed breed) {
        if (!breed.hasImage()) {
            return null;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/breeds/{slug}/image")
                .buildAndExpand(breed.slug())
                .toUriString();
    }
}
