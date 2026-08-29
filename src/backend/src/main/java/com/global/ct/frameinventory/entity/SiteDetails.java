package com.global.ct.frameinventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SiteDetails {

    @Column(name = "site_no", length = 100)
    private String siteNumber;

    @Column(name = "inventory_site_no", length = 100)
    private String inventorySiteNumber;

    @Column(name = "panel_number", length = 16)
    private String panelNumber;

    @Column(length = 150)
    private String station;

    @Column(length = 150)
    private String airport;

    protected SiteDetails() {
    }

    public SiteDetails(
        String siteNumber,
        String inventorySiteNumber,
        String panelNumber,
        String station,
        String airport
    ) {
        this.siteNumber = siteNumber;
        this.inventorySiteNumber = inventorySiteNumber;
        this.panelNumber = panelNumber;
        this.station = station;
        this.airport = airport;
    }

    public String getSiteNumber() {
        return siteNumber;
    }

    public String getInventorySiteNumber() {
        return inventorySiteNumber;
    }

    public String getPanelNumber() {
        return panelNumber;
    }

    public String getStation() {
        return station;
    }

    public String getAirport() {
        return airport;
    }
}
