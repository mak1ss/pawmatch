package com.pawmatch.catalog.domain;

import static com.pawmatch.catalog.domain.Breed.requireText;

/**
 * Who made a breed photo and under which licence it may be shown. Required for every image we serve.
 *
 * @param sourceUrl where the photo was obtained (e.g. a Wikimedia Commons file page)
 * @param author    photographer or rights holder
 * @param license   licence name, e.g. {@code CC BY-SA 4.0}
 */
public record ImageAttribution(String sourceUrl, String author, String license) {

    public ImageAttribution {
        requireText("image source_url", sourceUrl);
        requireText("image author", author);
        requireText("image license", license);
    }
}
