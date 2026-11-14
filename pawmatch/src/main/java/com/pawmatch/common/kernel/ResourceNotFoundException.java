package com.pawmatch.common.kernel;

/**
 * Thrown when a requested resource (breed, session, recommendation run, ...) does not exist. Mapped to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resource;
    private final String id;

    public ResourceNotFoundException(String resource, String id) {
        super(resource + " not found: " + id);
        this.resource = resource;
        this.id = id;
    }

    public String getResource() {
        return resource;
    }

    public String getId() {
        return id;
    }
}
