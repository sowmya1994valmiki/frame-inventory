package com.global.ct.frameinventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class IntegrationDetails {

    @Column(name = "broadsign_display_unit_id", length = 100)
    private String broadsignDisplayUnitId;

    @Column(name = "broadsign_frame_id", length = 100)
    private String broadsignFrameId;

    @Column(name = "broadsign_domain_id", length = 100)
    private String broadsignDomainId;

    @Column(name = "linked_frame_ids", length = 1000)
    private String linkedFrameIds;

    protected IntegrationDetails() {
    }

    public IntegrationDetails(
        String broadsignDisplayUnitId,
        String broadsignFrameId,
        String broadsignDomainId,
        String linkedFrameIds
    ) {
        this.broadsignDisplayUnitId = broadsignDisplayUnitId;
        this.broadsignFrameId = broadsignFrameId;
        this.broadsignDomainId = broadsignDomainId;
        this.linkedFrameIds = linkedFrameIds;
    }

    public String getBroadsignDisplayUnitId() {
        return broadsignDisplayUnitId;
    }

    public String getBroadsignFrameId() {
        return broadsignFrameId;
    }

    public String getBroadsignDomainId() {
        return broadsignDomainId;
    }

    public String getLinkedFrameIds() {
        return linkedFrameIds;
    }
}
