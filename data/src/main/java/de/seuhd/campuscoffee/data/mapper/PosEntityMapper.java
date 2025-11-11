// language: java
package de.seuhd.campuscoffee.data.mapper;

import de.seuhd.campuscoffee.data.persistence.AddressEntity;
import de.seuhd.campuscoffee.data.persistence.PosEntity;
import de.seuhd.campuscoffee.domain.model.Pos;
import org.mapstruct.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * MapStruct mapper for converting between domain models and JPA entities.
 */
@Mapper(componentModel = "spring")
@ConditionalOnMissingBean // prevent IntelliJ warning about duplicate beans
public interface PosEntityMapper {

    @Mapping(source = "address.street", target = "street")
    @Mapping(source = "address.postalCode", target = "postalCode")
    @Mapping(source = "address.city", target = "city")
    @Mapping(target = "houseNumber", expression = "java(mergeHouseNumber(source))")
    Pos fromEntity(PosEntity source);

    @Mapping(target = "address", expression = "java(splitHouseNumber(source, new AddressEntity()))")
    PosEntity toEntity(Pos source);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "address", expression = "java(splitHouseNumber(source, target.getAddress()))")
    void updateEntity(Pos source, @MappingTarget PosEntity target);

    @SuppressWarnings("unused")
    default String mergeHouseNumber(PosEntity source) {
        if (source == null || source.getAddress() == null || source.getAddress().getHouseNumber() == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf(source.getAddress().getHouseNumber()));
        Character suffix = source.getAddress().getHouseNumberSuffix();
        if (suffix != null) {
            sb.append(suffix);
        }
        return sb.toString();
    }

    @SuppressWarnings("unused")
    default AddressEntity splitHouseNumber(Pos source, AddressEntity addressEntity) {
        if (addressEntity == null) {
            addressEntity = new AddressEntity();
        }
        if (source == null) {
            return addressEntity;
        }

        addressEntity.setStreet(source.street());
        addressEntity.setCity(source.city());
        addressEntity.setPostalCode(source.postalCode());

        String hn = source.houseNumber();
        if (hn == null || hn.isBlank()) {
            // clear numeric/suffix if absent
            addressEntity.setHouseNumber(null);
            addressEntity.setHouseNumberSuffix(null);
            return addressEntity;
        }

        // numeric part is digits, suffix is remaining chars
        String numericPart = hn.replaceAll("[^0-9]", "");
        String suffixPart = hn.replaceAll("[0-9]", "");

        if (!numericPart.isEmpty()) {
            try {
                addressEntity.setHouseNumber(Integer.parseInt(numericPart));
            } catch (NumberFormatException e) {
                addressEntity.setHouseNumber(null);
            }
        } else {
            addressEntity.setHouseNumber(null);
        }

        if (suffixPart != null && !suffixPart.isEmpty()) {
            addressEntity.setHouseNumberSuffix(suffixPart.charAt(0));
        } else {
            addressEntity.setHouseNumberSuffix(null);
        }

        return addressEntity;
    }
}