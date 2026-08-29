package com.global.ct.frameinventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateFrameRequest(
    @NotBlank String mediaType,
    @NotBlank String format,
    String environment,
    @NotBlank String status,
    String statusReason,
    @NotNull @Valid LocationDto location,
    @NotNull @Valid SiteDto site,
    @Valid TechnicalDto technical,
    @Valid CommercialDto commercial,
    @Valid IntegrationDto integrations
) {
}
