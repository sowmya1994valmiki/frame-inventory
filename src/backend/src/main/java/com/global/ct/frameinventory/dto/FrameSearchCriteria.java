package com.global.ct.frameinventory.dto;

public record FrameSearchCriteria(
    String query,
    String status,
    String mediaType,
    String environment,
    String format,
    String region
) {
    public FrameSearchCriteria {
        query = normalize(query);
        status = normalize(status);
        mediaType = normalize(mediaType);
        environment = normalize(environment);
        format = normalize(format);
        region = normalize(region);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
