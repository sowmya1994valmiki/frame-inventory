package com.global.ct.frameinventory.dto;

public record TechnicalDto(
    String illuminationTypeId,
    Integer numberOfSlots,
    String sizeCode,
    String sizeGroupCode,
    String aspectRatioCode,
    String sizeCategory,
    Integer pixelHeight,
    Integer pixelWidth
) {
}
