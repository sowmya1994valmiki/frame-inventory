package com.global.ct.frameinventory.dto;

import java.time.Instant;

public record FrameSummaryResponse(
    String frameId,
    String mediaType,
    String format,
    String environment,
    String status,
    Instant modifiedDate,
    String address,
    String town,
    String region,
    String station,
    String airport
) {
}
