package com.global.ct.frameinventory.dto;

import java.time.Instant;

public record FrameResponse(
    String frameId,
    String mediaType,
    String format,
    String environment,
    String status,
    String statusReason,
    Instant createdDate,
    Instant modifiedDate,
    LocationDto location,
    SiteDto site,
    TechnicalDto technical,
    CommercialDto commercial,
    IntegrationDto integrations
) {
}
