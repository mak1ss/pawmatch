package com.pawmatch.catalog.adapter.out.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.pawmatch.catalog.application.port.out.BreedImageStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * {@link BreedImageStorage} reading files from a Spring resource location: bundled classpath files by default
 * ({@code classpath:catalog/images/}), or a directory via {@code file:/path/}. Loaded bytes are cached;
 * there are only a few dozen small images.
 * <p>
 * Storage keys are plain file names. Anything with path separators or {@code ..} is rejected.
 */
@Slf4j
@Component
class ResourceBreedImageStorage implements BreedImageStorage {

    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    private final ResourceLoader resourceLoader;
    private final String location;
    private final Map<String, byte[]> cache = new ConcurrentHashMap<>();

    ResourceBreedImageStorage(ResourceLoader resourceLoader,
                              @Value("${pawmatch.catalog.images.location:classpath:catalog/images/}") String location) {
        this.resourceLoader = resourceLoader;
        this.location = location.endsWith("/") ? location : location + "/";
    }

    @Override
    public Optional<byte[]> load(String storageKey) {
        if (storageKey == null || !SAFE_KEY.matcher(storageKey).matches() || storageKey.contains("..")) {
            log.warn("Rejected unsafe image storage key");
            return Optional.empty();
        }
        byte[] cached = cache.get(storageKey);
        if (cached != null) {
            return Optional.of(cached.clone());
        }
        Resource resource = resourceLoader.getResource(location + storageKey);
        if (!resource.exists()) {
            return Optional.empty();
        }
        try (InputStream in = resource.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            cache.put(storageKey, bytes);
            return Optional.of(bytes.clone());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read image " + storageKey, e);
        }
    }
}
