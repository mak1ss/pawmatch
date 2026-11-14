package com.pawmatch.catalog.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** Row of {@code breed_image}: photo metadata for one breed. */
@Entity
@Immutable
@Table(name = "breed_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class BreedImageJpaEntity {

    @Id
    private UUID breedId;

    private String storageKey;
    private String contentType;
    private String sourceUrl;
    private String author;
    private String license;
}
