package com.pawmatch.catalog.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** Row of {@code catalogue_dataset}: a breed dataset version loaded by a migration. */
@Entity
@Immutable
@Table(name = "catalogue_dataset")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CatalogueDatasetJpaEntity {

    @Id
    private String version;

    private Instant loadedAt;
}
