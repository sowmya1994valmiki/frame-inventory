package com.global.ct.frameinventory.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.global.ct.frameinventory.dto.CommercialDto;
import com.global.ct.frameinventory.dto.CreateFrameRequest;
import com.global.ct.frameinventory.dto.FrameResponse;
import com.global.ct.frameinventory.dto.FrameSummaryResponse;
import com.global.ct.frameinventory.dto.IntegrationDto;
import com.global.ct.frameinventory.dto.LocationDto;
import com.global.ct.frameinventory.dto.SiteDto;
import com.global.ct.frameinventory.dto.TechnicalDto;
import com.global.ct.frameinventory.dto.UpdateFrameRequest;
import com.global.ct.frameinventory.entity.CommercialDetails;
import com.global.ct.frameinventory.entity.Frame;
import com.global.ct.frameinventory.entity.IntegrationDetails;
import com.global.ct.frameinventory.entity.LocationDetails;
import com.global.ct.frameinventory.entity.SiteDetails;
import com.global.ct.frameinventory.entity.TechnicalDetails;

import org.springframework.stereotype.Component;

@Component
public class FrameMapper {

    public Frame toNewEntity(CreateFrameRequest request, LocalDateTime now) {
        return new Frame(
            request.frameId(), request.mediaType(), request.format(), request.environment(),
            request.status(), request.statusReason(), now, now,
            toLocation(request.location()), toSite(request.site()),
            toTechnical(request.technical()), toCommercial(request.commercial()),
            toIntegrations(request.integrations())
        );
    }

    public Frame toReplacement(Frame existing, UpdateFrameRequest request, LocalDateTime modifiedDate) {
        return new Frame(
            existing.getFrameId(), request.mediaType(), request.format(), request.environment(),
            request.status(), request.statusReason(), existing.getCreatedDate(), modifiedDate,
            toLocation(request.location()), toSite(request.site()),
            toTechnical(request.technical()), toCommercial(request.commercial()),
            toIntegrations(request.integrations())
        );
    }

    public FrameResponse toResponse(Frame frame) {
        return new FrameResponse(
            frame.getFrameId(), frame.getMediaType(), frame.getFormat(), frame.getEnvironment(),
            frame.getStatus(), frame.getStatusReason(), toInstant(frame.getCreatedDate()),
            toInstant(frame.getModifiedDate()), toLocationDto(frame.getLocation()),
            toSiteDto(frame.getSite()), toTechnicalDto(frame.getTechnical()),
            toCommercialDto(frame.getCommercial()), toIntegrationDto(frame.getIntegrations())
        );
    }

    public FrameSummaryResponse toSummary(Frame frame) {
        LocationDetails location = frame.getLocation();
        SiteDetails site = frame.getSite();
        return new FrameSummaryResponse(
            frame.getFrameId(), frame.getMediaType(), frame.getFormat(), frame.getEnvironment(),
            frame.getStatus(), toInstant(frame.getModifiedDate()),
            location == null ? null : location.getAddress(),
            location == null ? null : location.getTown(),
            location == null ? null : location.getRegion(),
            site == null ? null : site.getStation(),
            site == null ? null : site.getAirport()
        );
    }

    private LocationDetails toLocation(LocationDto dto) {
        if (dto == null) {
            return null;
        }
        return new LocationDetails(
            dto.postcode(), dto.postcodeArea(), dto.postcodeDistrict(), dto.postcodeSector(),
            dto.postcodeUnit(), dto.address(), dto.region(), dto.countryCode(), dto.town(),
            dto.longitude(), dto.latitude(), dto.distanceToClosestSchool(),
            dto.rawLocationPoint(), dto.locationId()
        );
    }

    private SiteDetails toSite(SiteDto dto) {
        if (dto == null) {
            return null;
        }
        return new SiteDetails(
            dto.siteNumber(), dto.inventorySiteNumber(), dto.panelNumber(), dto.station(), dto.airport()
        );
    }

    private TechnicalDetails toTechnical(TechnicalDto dto) {
        if (dto == null) {
            return null;
        }
        return new TechnicalDetails(
            dto.illuminationTypeId(), dto.numberOfSlots(), dto.sizeCode(), dto.sizeGroupCode(),
            dto.aspectRatioCode(), dto.sizeCategory(), dto.pixelHeight(), dto.pixelWidth()
        );
    }

    private CommercialDetails toCommercial(CommercialDto dto) {
        if (dto == null) {
            return null;
        }
        return new CommercialDetails(
            dto.impactWeight(), dto.productionRateCard(), dto.legacyProductionRateCard(),
            dto.pricingGrade(), dto.priceEntityId(), dto.premium()
        );
    }

    private IntegrationDetails toIntegrations(IntegrationDto dto) {
        if (dto == null) {
            return null;
        }
        return new IntegrationDetails(
            dto.broadsignDisplayUnitId(), dto.broadsignFrameId(),
            dto.broadsignDomainId(), dto.linkedFrameIds()
        );
    }

    private LocationDto toLocationDto(LocationDetails details) {
        if (details == null) {
            return null;
        }
        return new LocationDto(
            details.getPostcode(), details.getPostcodeArea(), details.getPostcodeDistrict(),
            details.getPostcodeSector(), details.getPostcodeUnit(), details.getAddress(),
            details.getRegion(), details.getCountryCode(), details.getTown(),
            details.getLongitude(), details.getLatitude(), details.getDistanceToClosestSchool(),
            details.getRawLocationPoint(), details.getLocationId()
        );
    }

    private SiteDto toSiteDto(SiteDetails details) {
        if (details == null) {
            return null;
        }
        return new SiteDto(
            details.getSiteNumber(), details.getInventorySiteNumber(), details.getPanelNumber(),
            details.getStation(), details.getAirport()
        );
    }

    private TechnicalDto toTechnicalDto(TechnicalDetails details) {
        if (details == null) {
            return null;
        }
        return new TechnicalDto(
            details.getIlluminationTypeId(), details.getNumberOfSlots(), details.getSizeCode(),
            details.getSizeGroupCode(), details.getAspectRatioCode(), details.getSizeCategory(),
            details.getPixelHeight(), details.getPixelWidth()
        );
    }

    private CommercialDto toCommercialDto(CommercialDetails details) {
        if (details == null) {
            return null;
        }
        return new CommercialDto(
            details.getImpactWeight(), details.getProductionRateCard(),
            details.getLegacyProductionRateCard(), details.getPricingGrade(),
            details.getPriceEntityId(), details.getPremium()
        );
    }

    private IntegrationDto toIntegrationDto(IntegrationDetails details) {
        if (details == null) {
            return null;
        }
        return new IntegrationDto(
            details.getBroadsignDisplayUnitId(), details.getBroadsignFrameId(),
            details.getBroadsignDomainId(), details.getLinkedFrameIds()
        );
    }

    private Instant toInstant(LocalDateTime value) {
        return value.toInstant(ZoneOffset.UTC);
    }
}
