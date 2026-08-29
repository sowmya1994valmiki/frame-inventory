package com.global.ct.frameinventory.dto;

import java.util.List;

public record FramePageResponse(
    List<FrameSummaryResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
