package com.global.ct.frameinventory.dto;

public record FrameCsvImportError(
    long rowNumber,
    String frameId,
    String reason
) {
}
