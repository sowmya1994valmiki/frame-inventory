package com.global.ct.frameinventory.frame;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TechnicalDetails {

    @Column(name = "illumination_type_id", length = 32)
    private String illuminationTypeId;

    @Column(name = "number_of_slots")
    private Integer numberOfSlots;

    @Column(name = "size_code", length = 100)
    private String sizeCode;

    @Column(name = "size_group_code", length = 100)
    private String sizeGroupCode;

    @Column(name = "aspect_ratio_code", length = 100)
    private String aspectRatioCode;

    @Column(name = "t_size", length = 32)
    private String sizeCategory;

    @Column(name = "pixel_height")
    private Integer pixelHeight;

    @Column(name = "pixel_width")
    private Integer pixelWidth;

    protected TechnicalDetails() {
    }

    public TechnicalDetails(
        String illuminationTypeId,
        Integer numberOfSlots,
        String sizeCode,
        String sizeGroupCode,
        String aspectRatioCode,
        String sizeCategory,
        Integer pixelHeight,
        Integer pixelWidth
    ) {
        this.illuminationTypeId = illuminationTypeId;
        this.numberOfSlots = numberOfSlots;
        this.sizeCode = sizeCode;
        this.sizeGroupCode = sizeGroupCode;
        this.aspectRatioCode = aspectRatioCode;
        this.sizeCategory = sizeCategory;
        this.pixelHeight = pixelHeight;
        this.pixelWidth = pixelWidth;
    }

    public String getIlluminationTypeId() {
        return illuminationTypeId;
    }

    public Integer getNumberOfSlots() {
        return numberOfSlots;
    }

    public String getSizeCode() {
        return sizeCode;
    }

    public String getSizeGroupCode() {
        return sizeGroupCode;
    }

    public String getAspectRatioCode() {
        return aspectRatioCode;
    }

    public String getSizeCategory() {
        return sizeCategory;
    }

    public Integer getPixelHeight() {
        return pixelHeight;
    }

    public Integer getPixelWidth() {
        return pixelWidth;
    }
}
