package com.pawmatch.catalog.application.port.in;

/** The version of the breed dataset currently loaded (e.g. {@code 1.0.0}), stored with every recommendation. */
public interface GetCatalogueVersionUseCase {

    String getCatalogueVersion();
}
