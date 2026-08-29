package com.global.ct.frameinventory.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

public record LocationDto(
    @NotBlank String postcode,
    String postcodeArea,
    String postcodeDistrict,
    String postcodeSector,
    String postcodeUnit,
    String address,
    @NotBlank String region,
    String countryCode,
    @NotBlank String town,
    BigDecimal longitude,
    BigDecimal latitude,
    Integer distanceToClosestSchool,
    String rawLocationPoint,
    String locationId
) {
}
