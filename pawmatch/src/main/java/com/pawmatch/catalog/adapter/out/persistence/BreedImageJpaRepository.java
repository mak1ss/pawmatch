package com.pawmatch.catalog.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface BreedImageJpaRepository extends JpaRepository<BreedImageJpaEntity, UUID> {
}
