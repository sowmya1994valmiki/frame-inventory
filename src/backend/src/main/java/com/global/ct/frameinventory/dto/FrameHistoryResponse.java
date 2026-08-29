package com.global.ct.frameinventory.dto;

import java.time.Instant;
import java.util.Map;

import com.global.ct.frameinventory.entity.FieldChange;
import com.global.ct.frameinventory.entity.HistoryEventType;
import com.global.ct.frameinventory.entity.HistorySource;

public record FrameHistoryResponse(
    HistoryEventType eventType,
    Instant occurredAt,
    HistorySource source,
    Map<String, FieldChange> changedFields
) {
}
