package com.global.ct.frameinventory.entity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "frame_history",
    indexes = @Index(
        name = "idx_frame_history_frame_timestamp",
        columnList = "frame_id, occurred_at"
    )
)
public class FrameHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "frame_id", nullable = false, updatable = false)
    private Frame frame;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 16)
    private HistoryEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HistorySource source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changed_fields", nullable = false, columnDefinition = "json")
    private Map<String, FieldChange> changedFields = new LinkedHashMap<>();

    protected FrameHistory() {
    }

    public FrameHistory(
        Frame frame,
        HistoryEventType eventType,
        Instant occurredAt,
        HistorySource source,
        Map<String, FieldChange> changedFields
    ) {
        this.frame = frame;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.source = source;
        this.changedFields = new LinkedHashMap<>(changedFields);
    }

    public Long getId() {
        return id;
    }

    public Frame getFrame() {
        return frame;
    }

    public HistoryEventType getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public HistorySource getSource() {
        return source;
    }

    public Map<String, FieldChange> getChangedFields() {
        return changedFields;
    }
}
