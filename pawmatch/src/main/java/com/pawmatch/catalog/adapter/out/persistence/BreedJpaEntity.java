package com.pawmatch.catalog.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** Row of {@code breed}. Read-only: the catalogue is written by Liquibase changesets only. */
@Entity
@Immutable
@Table(name = "breed")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class BreedJpaEntity {

    @Id
    private UUID id;

    private String slug;
    private String nameEn;
    private String nameUk;
    private String akcGroup;
    private String size;
    private String weight;
    private String height;
    private String lifeExpectancy;

    private int energyLevel;
    private int coatGroomingFrequency;
    private int sheddingLevel;
    private int trainabilityLevel;
    private int goodWithYoungChildren;
    private int goodWithOtherDogs;
    private int mentalStimulationNeeds;
    private int barkingLevel;
    private int adaptabilityLevel;
    private int affectionateWithFamily;
    private int opennessToStrangers;
    private int playfulnessLevel;
    private int droolingLevel;
    private int watchdogProtectiveNature;

    /** Pipe-separated, e.g. {@code Short|Medium}. */
    private String coatLength;
    /** Pipe-separated, e.g. {@code Silky|Wavy}. */
    private String coatType;

    @Column(precision = 4, scale = 1)
    private BigDecimal aloneToleranceHours;
    private Integer exerciseMinutesPerDay;
    private Integer noviceSuitability;
    private Integer goodWithCats;

    private String sourceUrl;
}
