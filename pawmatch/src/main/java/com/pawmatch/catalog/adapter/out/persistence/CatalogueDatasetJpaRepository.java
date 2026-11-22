package com.pawmatch.catalog.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface CatalogueDatasetJpaRepository extends JpaRepository<CatalogueDatasetJpaEntity, String> {

    Optional<CatalogueDatasetJpaEntity> findFirstByOrderByLoadedAtDescVersionDesc();
}
