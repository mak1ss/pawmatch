package com.pawmatch.catalog.application.port.in;

import com.pawmatch.catalog.domain.BreedImage;
import com.pawmatch.common.kernel.ResourceNotFoundException;

/** Returns the photo of a breed. */
public interface GetBreedImageUseCase {

    /**
     * @throws ResourceNotFoundException if the breed doesn't exist or has no photo
     */
    BreedImage getBreedImage(String slug);
}
