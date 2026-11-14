package com.pawmatch.catalog.domain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * A breed photo: metadata plus bytes. {@link #contentHash()} changes whenever the bytes change, so it can serve
 * as a cache validator.
 */
public final class BreedImage {

    private final BreedImageMetadata metadata;
    private final byte[] content;
    private final String contentHash;

    public BreedImage(BreedImageMetadata metadata, byte[] content) {
        this.metadata = Objects.requireNonNull(metadata, "metadata must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (content.length == 0) {
            throw new IllegalArgumentException("Image " + metadata.storageKey() + " is empty");
        }
        this.content = content.clone();
        this.contentHash = sha256(this.content);
    }

    public BreedImageMetadata metadata() {
        return metadata;
    }

    /** A copy of the image bytes. */
    public byte[] content() {
        return content.clone();
    }

    /** SHA-256 of the bytes, lowercase hex. */
    public String contentHash() {
        return contentHash;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BreedImage that && metadata.equals(that.metadata) && Arrays.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return 31 * metadata.hashCode() + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "BreedImage[" + metadata.storageKey() + ", " + content.length + " bytes, sha256=" + contentHash + "]";
    }
}
