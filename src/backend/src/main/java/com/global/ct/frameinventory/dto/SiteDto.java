package com.global.ct.frameinventory.dto;

import jakarta.validation.constraints.NotBlank;

public record SiteDto(
    @NotBlank String siteNumber,
    String inventorySiteNumber,
    String panelNumber,
    String station,
    String airport
) {
}
