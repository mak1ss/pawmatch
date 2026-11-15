package com.pawmatch.common.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Loads hand-written OpenAPI components (schemas, request bodies, examples) from {@code classpath:openapi/*.yaml}
 * and merges them into the spec generated from the controllers. This keeps verbose documentation, such as request
 * examples, out of Java code; controllers reference the components by name.
 * <p>
 * Each file holds a Components object: top-level keys {@code schemas}, {@code requestBodies}, {@code examples}, etc.
 * A component name defined in two files is an error.
 */
final class OpenApiComponentFiles {

    static final String LOCATION = "classpath*:openapi/*.yaml";

    private OpenApiComponentFiles() {
    }

    /** Reads and combines all component files. Fails fast on unreadable files or duplicate names. */
    static Components load(ResourcePatternResolver resolver) {
        Components combined = new Components();
        try {
            Resource[] files = resolver.getResources(LOCATION);
            Arrays.sort(files, Comparator.comparing(Resource::getDescription));
            for (Resource file : files) {
                try (InputStream in = file.getInputStream()) {
                    Components components = Yaml.mapper().readValue(in, Components.class);
                    if (components != null) {
                        mergeInto(combined, components, file.getDescription());
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read OpenAPI component files " + LOCATION, e);
        }
        return combined;
    }

    /** Adds {@code extra} to the spec's components. */
    static void mergeInto(OpenAPI openApi, Components extra) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        mergeInto(openApi.getComponents(), extra, "loaded OpenAPI component files");
    }

    private static void mergeInto(Components target, Components source, String origin) {
        merge(source, target, Components::getSchemas, Components::addSchemas, "schema", origin);
        merge(source, target, Components::getRequestBodies, Components::addRequestBodies, "request body", origin);
        merge(source, target, Components::getExamples, Components::addExamples, "example", origin);
        merge(source, target, Components::getResponses, Components::addResponses, "response", origin);
        merge(source, target, Components::getParameters, Components::addParameters, "parameter", origin);
    }

    private static <T> void merge(Components source, Components target, Function<Components, Map<String, T>> getter,
                                  TriConsumer<Components, String, T> adder, String kind, String origin) {
        Map<String, T> items = getter.apply(source);
        if (items == null) {
            return;
        }
        Map<String, T> existing = getter.apply(target);
        items.forEach((name, item) -> {
            if (existing != null && existing.containsKey(name)) {
                throw new IllegalStateException("Duplicate OpenAPI " + kind + " '" + name + "' in " + origin);
            }
            adder.accept(target, name, item);
        });
    }

    @FunctionalInterface
    private interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }
}
