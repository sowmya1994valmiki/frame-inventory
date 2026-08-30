package com.global.ct.frameinventory.service;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.global.ct.frameinventory.dto.CommercialDto;
import com.global.ct.frameinventory.dto.CreateFrameRequest;
import com.global.ct.frameinventory.dto.FrameCsvImportError;
import com.global.ct.frameinventory.dto.FrameCsvImportSummary;
import com.global.ct.frameinventory.dto.IntegrationDto;
import com.global.ct.frameinventory.dto.LocationDto;
import com.global.ct.frameinventory.dto.SiteDto;
import com.global.ct.frameinventory.dto.TechnicalDto;
import com.global.ct.frameinventory.exception.DuplicateFrameException;
import com.global.ct.frameinventory.exception.InvalidCsvFileException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Service
public class FrameCsvImportService {

    private static final Set<String> REQUIRED_HEADERS = Set.of(
        "frame_id", "type_classic_digital", "format", "status",
        "postcode", "site_no", "region", "town"
    );

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
        .setHeader()
        .setSkipHeaderRecord(true)
        .setIgnoreEmptyLines(true)
        .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
        .get();

    private final FrameService frameService;
    private final Validator validator;

    public FrameCsvImportService(FrameService frameService, Validator validator) {
        this.frameService = frameService;
        this.validator = validator;
    }

    public FrameCsvImportSummary importCsv(MultipartFile file) {
        List<CSVRecord> records = parse(file);
        Set<String> seenFrameIds = new HashSet<>();
        List<FrameCsvImportError> errors = new ArrayList<>();
        int created = 0;
        int duplicates = 0;
        int failed = 0;

        for (CSVRecord record : records) {
            long rowNumber = record.getRecordNumber() + 1;
            String frameId = value(record, "frame_id");

            if (frameId != null && !seenFrameIds.add(frameId)) {
                duplicates++;
                errors.add(new FrameCsvImportError(
                    rowNumber,
                    frameId,
                    "frameId appears more than once in the CSV"
                ));
                continue;
            }

            try {
                CreateFrameRequest request = toRequest(record);
                validate(request);
                frameService.createImportedFrame(request);
                created++;
            } catch (DuplicateFrameException exception) {
                duplicates++;
                errors.add(new FrameCsvImportError(rowNumber, frameId, exception.getMessage()));
            } catch (RowImportException exception) {
                failed++;
                errors.add(new FrameCsvImportError(rowNumber, frameId, exception.getMessage()));
            } catch (DataAccessException exception) {
                failed++;
                errors.add(new FrameCsvImportError(rowNumber, frameId, "Frame could not be persisted"));
            }
        }

        return new FrameCsvImportSummary(records.size(), created, duplicates, failed, errors);
    }

    private List<CSVRecord> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidCsvFileException("CSV file is empty");
        }

        String content = decode(file);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        if (content.isBlank()) {
            throw new InvalidCsvFileException("CSV file is empty");
        }

        try (CSVParser parser = CSV_FORMAT.parse(new StringReader(content))) {
            Map<String, Integer> headers = parser.getHeaderMap();
            Set<String> missingHeaders = REQUIRED_HEADERS.stream()
                .filter(header -> !headers.containsKey(header))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
            if (!missingHeaders.isEmpty()) {
                throw new InvalidCsvFileException(
                    "CSV is missing required headers: " + String.join(", ", missingHeaders)
                );
            }

            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                throw new InvalidCsvFileException("CSV contains no data rows");
            }
            for (CSVRecord record : records) {
                if (record.size() != headers.size()) {
                    throw new InvalidCsvFileException(
                        "CSV row " + (record.getRecordNumber() + 1)
                            + " has a different number of columns than the header"
                    );
                }
            }
            return records;
        } catch (InvalidCsvFileException exception) {
            throw exception;
        } catch (IOException | UncheckedIOException | IllegalArgumentException exception) {
            throw new InvalidCsvFileException("CSV content is malformed", exception);
        }
    }

    private String decode(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        } catch (CharacterCodingException exception) {
            throw new InvalidCsvFileException("CSV content is not valid UTF-8", exception);
        } catch (IOException exception) {
            throw new InvalidCsvFileException("CSV file could not be read", exception);
        }
    }

    private CreateFrameRequest toRequest(CSVRecord record) {
        return new CreateFrameRequest(
            value(record, "frame_id"),
            value(record, "type_classic_digital"),
            value(record, "format"),
            value(record, "environment"),
            value(record, "status"),
            value(record, "status_reason"),
            new LocationDto(
                value(record, "postcode"),
                value(record, "postcode_area"),
                value(record, "postcode_district"),
                value(record, "postcode_sector"),
                value(record, "postcode_unit"),
                value(record, "address"),
                value(record, "region"),
                value(record, "country_code"),
                value(record, "town"),
                decimal(record, "longitude"),
                decimal(record, "latitude"),
                integer(record, "distance_to_closest_school"),
                value(record, "location"),
                value(record, "location_id")
            ),
            new SiteDto(
                value(record, "site_no"),
                value(record, "inventory_site_no"),
                value(record, "panel_number"),
                value(record, "station"),
                value(record, "airport")
            ),
            new TechnicalDto(
                value(record, "illumination_type_id"),
                integer(record, "number_of_slots"),
                value(record, "size_code"),
                value(record, "size_group_code"),
                value(record, "aspect_ratio_code"),
                value(record, "t_size"),
                integer(record, "pixel_height"),
                integer(record, "pixel_width")
            ),
            new CommercialDto(
                decimal(record, "impact_weight"),
                value(record, "production_rate_card"),
                value(record, "production_rate_card_legacy"),
                value(record, "pricing_grade"),
                value(record, "price_entity_id"),
                bool(record, "premium")
            ),
            new IntegrationDto(
                value(record, "broadsign_display_unit_id"),
                value(record, "broadsign_frame_id"),
                value(record, "broadsign_domain_id"),
                value(record, "linked_frame_ids")
            )
        );
    }

    private void validate(CreateFrameRequest request) {
        String reason = validator.validate(request).stream()
            .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
            .map(this::validationMessage)
            .collect(Collectors.joining("; "));
        if (!reason.isEmpty()) {
            throw new RowImportException(reason);
        }
    }

    private String validationMessage(ConstraintViolation<CreateFrameRequest> violation) {
        return violation.getPropertyPath() + " " + violation.getMessage();
    }

    private String value(CSVRecord record, String header) {
        if (!record.isMapped(header)) {
            return null;
        }
        String value = record.get(header).trim();
        return value.isEmpty() ? null : value;
    }

    private Integer integer(CSVRecord record, String header) {
        String value = value(record, header);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new RowImportException(header + " must be a whole number");
        }
    }

    private BigDecimal decimal(CSVRecord record, String header) {
        String value = value(record, header);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new RowImportException(header + " must be a decimal number");
        }
    }

    private Boolean bool(CSVRecord record, String header) {
        String value = value(record, header);
        if (value == null) {
            return null;
        }
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "1", "true" -> true;
            case "0", "false" -> false;
            default -> throw new RowImportException(header + " must be 0, 1, true, or false");
        };
    }

    private static class RowImportException extends RuntimeException {

        RowImportException(String message) {
            super(message);
        }
    }
}
