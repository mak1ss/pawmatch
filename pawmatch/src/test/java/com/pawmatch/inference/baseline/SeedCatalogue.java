package com.pawmatch.inference.baseline;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pawmatch.recommendation.domain.BreedFeatures;
import com.pawmatch.recommendation.domain.SizeCategory;

/**
 * The real v1.0.0 breed catalogue as engine input, read from the Liquibase seed CSV, so engine tests need no
 * database. Includes a minimal CSV reader (quoted fields with commas) sufficient for that file.
 */
final class SeedCatalogue {

    private static final String SEED = "/db/changelog/data/breeds-v1.0.0.csv";
    private static List<BreedFeatures> cached;

    private SeedCatalogue() {
    }

    static synchronized List<BreedFeatures> breeds() {
        if (cached == null) {
            cached = load();
        }
        return cached;
    }

    static BreedFeatures breed(String breedId) {
        return breeds().stream().filter(b -> b.breedId().equals(breedId)).findFirst().orElseThrow();
    }

    private static List<BreedFeatures> load() {
        try (InputStream in = SeedCatalogue.class.getResourceAsStream(SEED)) {
            List<String> lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            List<String> header = parse(lines.getFirst());
            List<BreedFeatures> breeds = new ArrayList<>();
            for (String line : lines.subList(1, lines.size())) {
                List<String> values = parse(line);
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < header.size(); i++) {
                    row.put(header.get(i), values.get(i));
                }
                breeds.add(new BreedFeatures(row.get("slug"), row.get("name_uk"), row.get("name_en"),
                        SizeCategory.fromCode(row.get("size")), num(row, "energy_level"), num(row, "shedding_level"),
                        num(row, "coat_grooming_frequency"), num(row, "mental_stimulation_needs"),
                        num(row, "barking_level"), num(row, "good_with_young_children"),
                        num(row, "good_with_other_dogs"), row.get("source_url")));
            }
            return List.copyOf(breeds);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int num(Map<String, String> row, String key) {
        return Integer.parseInt(row.get(key));
    }

    private static List<String> parse(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
