package com.global.ct.frameinventory.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.global.ct.frameinventory.dto.CreateFrameRequest;
import com.global.ct.frameinventory.dto.FrameHistoryResponse;
import com.global.ct.frameinventory.dto.FramePageResponse;
import com.global.ct.frameinventory.dto.FrameResponse;
import com.global.ct.frameinventory.dto.FrameSearchCriteria;
import com.global.ct.frameinventory.dto.FrameSummaryResponse;
import com.global.ct.frameinventory.dto.UpdateFrameRequest;
import com.global.ct.frameinventory.entity.CommercialDetails;
import com.global.ct.frameinventory.entity.FieldChange;
import com.global.ct.frameinventory.entity.Frame;
import com.global.ct.frameinventory.entity.FrameHistory;
import com.global.ct.frameinventory.entity.HistoryEventType;
import com.global.ct.frameinventory.entity.HistorySource;
import com.global.ct.frameinventory.entity.IntegrationDetails;
import com.global.ct.frameinventory.entity.LocationDetails;
import com.global.ct.frameinventory.entity.SiteDetails;
import com.global.ct.frameinventory.entity.TechnicalDetails;
import com.global.ct.frameinventory.exception.DuplicateFrameException;
import com.global.ct.frameinventory.exception.FrameNotFoundException;
import com.global.ct.frameinventory.mapper.FrameMapper;
import com.global.ct.frameinventory.repository.FrameHistoryRepository;
import com.global.ct.frameinventory.repository.FrameRepository;
import com.global.ct.frameinventory.specification.FrameSpecifications;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@Service
public class FrameService {

    private final FrameRepository repository;
    private final FrameHistoryRepository historyRepository;
    private final FrameMapper mapper;
    private final Clock clock;
    private final EntityManager entityManager;

    public FrameService(
        FrameRepository repository,
        FrameHistoryRepository historyRepository,
        FrameMapper mapper,
        Clock clock,
        EntityManager entityManager
    ) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.mapper = mapper;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Transactional
    public FrameResponse createFrame(CreateFrameRequest request) {
        if (repository.existsById(request.frameId())) {
            throw new DuplicateFrameException(request.frameId());
        }

        Instant occurredAt = now();
        Frame frame = mapper.toNewEntity(request, toLocalDateTime(occurredAt));
        Frame saved;
        try {
            saved = repository.saveAndFlush(frame);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateKey(exception)) {
                throw new DuplicateFrameException(request.frameId());
            }
            throw exception;
        }
        entityManager.refresh(saved);
        historyRepository.save(new FrameHistory(
            saved,
            HistoryEventType.CREATED,
            occurredAt,
            HistorySource.MANUAL,
            Map.of()
        ));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public FrameResponse getFrame(String frameId) {
        return repository.findById(frameId)
            .map(mapper::toResponse)
            .orElseThrow(() -> new FrameNotFoundException(frameId));
    }

    @Transactional(readOnly = true)
    public List<FrameHistoryResponse> getFrameHistory(String frameId) {
        if (!repository.existsById(frameId)) {
            throw new FrameNotFoundException(frameId);
        }
        return historyRepository.findByFrameFrameIdOrderByOccurredAtDescIdDesc(frameId).stream()
            .map(history -> new FrameHistoryResponse(
                history.getEventType(),
                history.getOccurredAt(),
                history.getSource(),
                history.getChangedFields()
            ))
            .toList();
    }

    @Transactional
    public FrameResponse replaceFrame(String frameId, UpdateFrameRequest request) {
        Frame existing = repository.findById(frameId)
            .orElseThrow(() -> new FrameNotFoundException(frameId));
        Instant occurredAt = now();
        Frame replacement = mapper.toReplacement(existing, request, toLocalDateTime(occurredAt));
        Map<String, FieldChange> changes = changedFields(existing, replacement);
        if (changes.isEmpty()) {
            return mapper.toResponse(existing);
        }

        Frame saved = repository.saveAndFlush(replacement);
        entityManager.refresh(saved);
        historyRepository.save(new FrameHistory(
            saved,
            HistoryEventType.UPDATED,
            occurredAt,
            HistorySource.MANUAL,
            changes
        ));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public FramePageResponse searchFrames(FrameSearchCriteria criteria, Pageable pageable) {
        Page<FrameSummaryResponse> result = repository
            .findAll(FrameSpecifications.matching(criteria), pageable)
            .map(mapper::toSummary);
        return new FramePageResponse(
            result.getContent(), result.getNumber(), result.getSize(),
            result.getTotalElements(), result.getTotalPages()
        );
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private LocalDateTime toLocalDateTime(Instant value) {
        return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private Map<String, FieldChange> changedFields(Frame oldFrame, Frame newFrame) {
        Map<String, FieldChange> changes = new LinkedHashMap<>();
        addChange(changes, "mediaType", oldFrame.getMediaType(), newFrame.getMediaType());
        addChange(changes, "format", oldFrame.getFormat(), newFrame.getFormat());
        addChange(changes, "environment", oldFrame.getEnvironment(), newFrame.getEnvironment());
        addChange(changes, "status", oldFrame.getStatus(), newFrame.getStatus());
        addChange(changes, "statusReason", oldFrame.getStatusReason(), newFrame.getStatusReason());
        addLocationChanges(changes, oldFrame.getLocation(), newFrame.getLocation());
        addSiteChanges(changes, oldFrame.getSite(), newFrame.getSite());
        addTechnicalChanges(changes, oldFrame.getTechnical(), newFrame.getTechnical());
        addCommercialChanges(changes, oldFrame.getCommercial(), newFrame.getCommercial());
        addIntegrationChanges(changes, oldFrame.getIntegrations(), newFrame.getIntegrations());
        return changes;
    }

    private void addLocationChanges(
        Map<String, FieldChange> changes,
        LocationDetails oldValue,
        LocationDetails newValue
    ) {
        addNestedChange(changes, "location.postcode", oldValue, newValue, LocationDetails::getPostcode);
        addNestedChange(changes, "location.postcodeArea", oldValue, newValue, LocationDetails::getPostcodeArea);
        addNestedChange(
            changes,
            "location.postcodeDistrict",
            oldValue,
            newValue,
            LocationDetails::getPostcodeDistrict
        );
        addNestedChange(
            changes,
            "location.postcodeSector",
            oldValue,
            newValue,
            LocationDetails::getPostcodeSector
        );
        addNestedChange(changes, "location.postcodeUnit", oldValue, newValue, LocationDetails::getPostcodeUnit);
        addNestedChange(changes, "location.address", oldValue, newValue, LocationDetails::getAddress);
        addNestedChange(changes, "location.region", oldValue, newValue, LocationDetails::getRegion);
        addNestedChange(changes, "location.countryCode", oldValue, newValue, LocationDetails::getCountryCode);
        addNestedChange(changes, "location.town", oldValue, newValue, LocationDetails::getTown);
        addNestedChange(changes, "location.longitude", oldValue, newValue, LocationDetails::getLongitude);
        addNestedChange(changes, "location.latitude", oldValue, newValue, LocationDetails::getLatitude);
        addNestedChange(
            changes,
            "location.distanceToClosestSchool",
            oldValue,
            newValue,
            LocationDetails::getDistanceToClosestSchool
        );
        addNestedChange(
            changes,
            "location.rawLocationPoint",
            oldValue,
            newValue,
            LocationDetails::getRawLocationPoint
        );
        addNestedChange(changes, "location.locationId", oldValue, newValue, LocationDetails::getLocationId);
    }

    private void addSiteChanges(
        Map<String, FieldChange> changes,
        SiteDetails oldValue,
        SiteDetails newValue
    ) {
        addNestedChange(changes, "site.siteNumber", oldValue, newValue, SiteDetails::getSiteNumber);
        addNestedChange(
            changes,
            "site.inventorySiteNumber",
            oldValue,
            newValue,
            SiteDetails::getInventorySiteNumber
        );
        addNestedChange(changes, "site.panelNumber", oldValue, newValue, SiteDetails::getPanelNumber);
        addNestedChange(changes, "site.station", oldValue, newValue, SiteDetails::getStation);
        addNestedChange(changes, "site.airport", oldValue, newValue, SiteDetails::getAirport);
    }

    private void addTechnicalChanges(
        Map<String, FieldChange> changes,
        TechnicalDetails oldValue,
        TechnicalDetails newValue
    ) {
        addNestedChange(
            changes,
            "technical.illuminationTypeId",
            oldValue,
            newValue,
            TechnicalDetails::getIlluminationTypeId
        );
        addNestedChange(
            changes,
            "technical.numberOfSlots",
            oldValue,
            newValue,
            TechnicalDetails::getNumberOfSlots
        );
        addNestedChange(changes, "technical.sizeCode", oldValue, newValue, TechnicalDetails::getSizeCode);
        addNestedChange(
            changes,
            "technical.sizeGroupCode",
            oldValue,
            newValue,
            TechnicalDetails::getSizeGroupCode
        );
        addNestedChange(
            changes,
            "technical.aspectRatioCode",
            oldValue,
            newValue,
            TechnicalDetails::getAspectRatioCode
        );
        addNestedChange(
            changes,
            "technical.sizeCategory",
            oldValue,
            newValue,
            TechnicalDetails::getSizeCategory
        );
        addNestedChange(
            changes,
            "technical.pixelHeight",
            oldValue,
            newValue,
            TechnicalDetails::getPixelHeight
        );
        addNestedChange(
            changes,
            "technical.pixelWidth",
            oldValue,
            newValue,
            TechnicalDetails::getPixelWidth
        );
    }

    private void addCommercialChanges(
        Map<String, FieldChange> changes,
        CommercialDetails oldValue,
        CommercialDetails newValue
    ) {
        addNestedChange(
            changes,
            "commercial.impactWeight",
            oldValue,
            newValue,
            CommercialDetails::getImpactWeight
        );
        addNestedChange(
            changes,
            "commercial.productionRateCard",
            oldValue,
            newValue,
            CommercialDetails::getProductionRateCard
        );
        addNestedChange(
            changes,
            "commercial.legacyProductionRateCard",
            oldValue,
            newValue,
            CommercialDetails::getLegacyProductionRateCard
        );
        addNestedChange(
            changes,
            "commercial.pricingGrade",
            oldValue,
            newValue,
            CommercialDetails::getPricingGrade
        );
        addNestedChange(
            changes,
            "commercial.priceEntityId",
            oldValue,
            newValue,
            CommercialDetails::getPriceEntityId
        );
        addNestedChange(changes, "commercial.premium", oldValue, newValue, CommercialDetails::getPremium);
    }

    private void addIntegrationChanges(
        Map<String, FieldChange> changes,
        IntegrationDetails oldValue,
        IntegrationDetails newValue
    ) {
        addNestedChange(
            changes,
            "integrations.broadsignDisplayUnitId",
            oldValue,
            newValue,
            IntegrationDetails::getBroadsignDisplayUnitId
        );
        addNestedChange(
            changes,
            "integrations.broadsignFrameId",
            oldValue,
            newValue,
            IntegrationDetails::getBroadsignFrameId
        );
        addNestedChange(
            changes,
            "integrations.broadsignDomainId",
            oldValue,
            newValue,
            IntegrationDetails::getBroadsignDomainId
        );
        addNestedChange(
            changes,
            "integrations.linkedFrameIds",
            oldValue,
            newValue,
            IntegrationDetails::getLinkedFrameIds
        );
    }

    private <T, R> void addNestedChange(
        Map<String, FieldChange> changes,
        String field,
        T oldContainer,
        T newContainer,
        Function<T, R> getter
    ) {
        R oldValue = oldContainer == null ? null : getter.apply(oldContainer);
        R newValue = newContainer == null ? null : getter.apply(newContainer);
        addChange(changes, field, oldValue, newValue);
    }

    private void addChange(
        Map<String, FieldChange> changes,
        String field,
        Object oldValue,
        Object newValue
    ) {
        if (!valuesEqual(oldValue, newValue)) {
            changes.put(field, new FieldChange(asHistoryValue(oldValue), asHistoryValue(newValue)));
        }
    }

    private boolean valuesEqual(Object oldValue, Object newValue) {
        if (oldValue instanceof BigDecimal oldDecimal && newValue instanceof BigDecimal newDecimal) {
            return oldDecimal.compareTo(newDecimal) == 0;
        }
        return Objects.equals(oldValue, newValue);
    }

    private String asHistoryValue(Object value) {
        return value instanceof BigDecimal decimal ? decimal.toPlainString() : Objects.toString(value, null);
    }

    private boolean isDuplicateKey(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof DuplicateKeyException) {
                return true;
            }
            if (current instanceof SQLException sqlException
                && ("23505".equals(sqlException.getSQLState()) || sqlException.getErrorCode() == 1062)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
