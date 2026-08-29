package com.global.ct.frameinventory.dto;

public record IntegrationDto(
    String broadsignDisplayUnitId,
    String broadsignFrameId,
    String broadsignDomainId,
    String linkedFrameIds
) {
}
