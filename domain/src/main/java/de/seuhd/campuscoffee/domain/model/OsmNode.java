package de.seuhd.campuscoffee.domain.model;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents an OpenStreetMap node with relevant Point of Sale information.
 * This is the domain model for OSM data before it is converted to a POS object.
 *
 * @param nodeId The OpenStreetMap node ID.
 * @param name       The name of the POI (tag:name).
 * @param street     Street (addr:street)
 * @param houseNumber House number (addr:housenumber) - raw string, may include suffix.
 * @param postalCode Postal code (addr:postcode)
 * @param city       City (addr:city)
 * @param type       Mapped PosType (from amenity/shop)
 * @param campus     Optional campus mapping (from campus or seuhd:campus)
 */
@Builder
public record OsmNode(@NonNull Long nodeId,
                      @Nullable String name,
                      @Nullable String street,
                      @Nullable String houseNumber,
                      @Nullable Integer postalCode,
                      @Nullable String city,
                      @Nullable PosType type,
                      @Nullable CampusType campus) {
}