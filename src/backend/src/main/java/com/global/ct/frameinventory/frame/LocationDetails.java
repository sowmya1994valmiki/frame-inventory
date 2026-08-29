package com.global.ct.frameinventory.frame;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class LocationDetails {

    @Column(length = 16)
    private String postcode;

    @Column(name = "postcode_area", length = 8)
    private String postcodeArea;

    @Column(name = "postcode_district", length = 8)
    private String postcodeDistrict;

    @Column(name = "postcode_sector", length = 8)
    private String postcodeSector;

    @Column(name = "postcode_unit", length = 8)
    private String postcodeUnit;

    @Column(length = 1000)
    private String address;

    @Column(length = 100)
    private String region;

    @Column(name = "country_code", length = 8)
    private String countryCode;

    @Column(length = 100)
    private String town;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal latitude;

    @Column(name = "distance_to_closest_school")
    private Integer distanceToClosestSchool;

    @Column(name = "location", length = 100)
    private String rawLocationPoint;

    @Column(name = "location_id", length = 100)
    private String locationId;

    protected LocationDetails() {
    }

    public LocationDetails(
        String postcode,
        String postcodeArea,
        String postcodeDistrict,
        String postcodeSector,
        String postcodeUnit,
        String address,
        String region,
        String countryCode,
        String town,
        BigDecimal longitude,
        BigDecimal latitude,
        Integer distanceToClosestSchool,
        String rawLocationPoint,
        String locationId
    ) {
        this.postcode = postcode;
        this.postcodeArea = postcodeArea;
        this.postcodeDistrict = postcodeDistrict;
        this.postcodeSector = postcodeSector;
        this.postcodeUnit = postcodeUnit;
        this.address = address;
        this.region = region;
        this.countryCode = countryCode;
        this.town = town;
        this.longitude = longitude;
        this.latitude = latitude;
        this.distanceToClosestSchool = distanceToClosestSchool;
        this.rawLocationPoint = rawLocationPoint;
        this.locationId = locationId;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getPostcodeArea() {
        return postcodeArea;
    }

    public String getPostcodeDistrict() {
        return postcodeDistrict;
    }

    public String getPostcodeSector() {
        return postcodeSector;
    }

    public String getPostcodeUnit() {
        return postcodeUnit;
    }

    public String getAddress() {
        return address;
    }

    public String getRegion() {
        return region;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getTown() {
        return town;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public Integer getDistanceToClosestSchool() {
        return distanceToClosestSchool;
    }

    public String getRawLocationPoint() {
        return rawLocationPoint;
    }

    public String getLocationId() {
        return locationId;
    }
}
