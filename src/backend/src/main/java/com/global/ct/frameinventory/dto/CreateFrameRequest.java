package com.global.ct.frameinventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFrameRequest(
    @NotBlank @Size(max = 64) String frameId,
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
    public CreateFrameRequest {
        frameId = frameId == null ? null : frameId.trim();
    }
}
