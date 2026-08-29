package com.global.ct.frameinventory.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "frames")
public class Frame {

    @Id
    @Column(name = "frame_id", nullable = false, updatable = false, length = 64)
    private String frameId;

    @Column(name = "type_classic_digital", nullable = false, length = 32)
    private String mediaType;

    @Column(nullable = false, length = 64)
    private String format;

    @Column(length = 32)
    private String environment;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;

    @Embedded
    private LocationDetails location;

    @Embedded
    private SiteDetails site;

    @Embedded
    private TechnicalDetails technical;

    @Embedded
    private CommercialDetails commercial;

    @Embedded
    private IntegrationDetails integrations;

    protected Frame() {
    }

    public Frame(
        String frameId,
        String mediaType,
        String format,
        String environment,
        String status,
        String statusReason,
        LocalDateTime createdDate,
        LocalDateTime modifiedDate,
        LocationDetails location,
        SiteDetails site,
        TechnicalDetails technical,
        CommercialDetails commercial,
        IntegrationDetails integrations
    ) {
        this.frameId = frameId;
        this.mediaType = mediaType;
        this.format = format;
        this.environment = environment;
        this.status = status;
        this.statusReason = statusReason;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.location = location;
        this.site = site;
        this.technical = technical;
        this.commercial = commercial;
        this.integrations = integrations;
    }

    public String getFrameId() {
        return frameId;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getFormat() {
        return format;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public LocationDetails getLocation() {
        return location;
    }

    public SiteDetails getSite() {
        return site;
    }

    public TechnicalDetails getTechnical() {
        return technical;
    }

    public CommercialDetails getCommercial() {
        return commercial;
    }

    public IntegrationDetails getIntegrations() {
        return integrations;
    }
}
