package com.pawmatch.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class BreedImageTest {

    @Test
    void computesSha256OfContent() {
        BreedImage image = new BreedImage(TestBreeds.image("a.webp"), "abc".getBytes());

        // SHA-256("abc")
        assertThat(image.contentHash()).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void contentIsDefensivelyCopied() {
        byte[] bytes = {1, 2, 3};
        BreedImage image = new BreedImage(TestBreeds.image("a.webp"), bytes);
        bytes[0] = 9;
        image.content()[1] = 9;

        assertThat(image.content()).containsExactly(1, 2, 3);
    }

    @Test
    void rejectsEmptyContentAndUnsupportedTypes() {
        assertThatIllegalArgumentException().isThrownBy(() -> new BreedImage(TestBreeds.image("a.webp"), new byte[0]));
        assertThatIllegalArgumentException().isThrownBy(() -> new BreedImageMetadata("a.gif", "image/gif",
                new ImageAttribution("https://example.org", "Jane Doe", "CC0")));
    }
}
