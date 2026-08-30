package com.global.ct.frameinventory.dto;

import java.util.List;

public record FrameCsvImportSummary(
    int totalRows,
    int created,
    int duplicates,
    int failed,
    List<FrameCsvImportError> errors
) {
    public FrameCsvImportSummary {
        errors = List.copyOf(errors);
    }
}
