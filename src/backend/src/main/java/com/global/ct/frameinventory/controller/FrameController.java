package com.global.ct.frameinventory.controller;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.global.ct.frameinventory.dto.CreateFrameRequest;
import com.global.ct.frameinventory.dto.FrameCsvImportSummary;
import com.global.ct.frameinventory.dto.FrameHistoryResponse;
import com.global.ct.frameinventory.dto.FramePageResponse;
import com.global.ct.frameinventory.dto.FrameResponse;
import com.global.ct.frameinventory.dto.FrameSearchCriteria;
import com.global.ct.frameinventory.dto.UpdateFrameRequest;
import com.global.ct.frameinventory.exception.InvalidFrameRequestException;
import com.global.ct.frameinventory.service.FrameCsvImportService;
import com.global.ct.frameinventory.service.FrameService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/frames")
public class FrameController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameController.class);

    private static final Set<String> SORT_FIELDS = Set.of(
        "frameId", "createdDate", "modifiedDate", "mediaType", "format", "environment", "status"
    );

    private final FrameService service;
    private final FrameCsvImportService csvImportService;

    public FrameController(FrameService service, FrameCsvImportService csvImportService) {
        this.service = service;
        this.csvImportService = csvImportService;
    }

    @PostMapping
    public ResponseEntity<FrameResponse> create(@Valid @RequestBody CreateFrameRequest request) {
        FrameResponse response = service.createFrame(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{frameId}")
            .buildAndExpand(response.frameId())
            .encode()
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FrameCsvImportSummary importCsv(@RequestPart("file") MultipartFile file) {
        LOGGER.info("CSV import requested sizeBytes={}", file.getSize());
        return csvImportService.importCsv(file);
    }

    @GetMapping("/{frameId}")
    public FrameResponse get(@PathVariable String frameId) {
        return service.getFrame(frameId);
    }

    @GetMapping("/{frameId}/history")
    public List<FrameHistoryResponse> getHistory(@PathVariable String frameId) {
        return service.getFrameHistory(frameId);
    }

    @PutMapping("/{frameId}")
    public FrameResponse replace(
        @PathVariable String frameId,
        @Valid @RequestBody UpdateFrameRequest request
    ) {
        return service.replaceFrame(frameId, request);
    }

    @GetMapping
    public FramePageResponse search(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "modifiedDate,desc") String sort,
        @RequestParam(name = "q", required = false) String query,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String mediaType,
        @RequestParam(required = false) String environment,
        @RequestParam(required = false) String format,
        @RequestParam(required = false) String region
    ) {
        validatePage(page, size);
        PageRequest pageRequest = PageRequest.of(page, size, parseSort(sort));
        return service.searchFrames(
            new FrameSearchCriteria(query, status, mediaType, environment, format, region),
            pageRequest
        );
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new InvalidFrameRequestException("page must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw new InvalidFrameRequestException("size must be between 1 and 100");
        }
    }

    private Sort parseSort(String value) {
        String[] parts = value.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0])) {
            throw new InvalidFrameRequestException("sort must use an allowed field and direction");
        }

        Sort.Direction direction;
        try {
            direction = Sort.Direction.valueOf(parts[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidFrameRequestException("sort direction must be asc or desc");
        }

        Sort primary = Sort.by(direction, parts[0]);
        return "frameId".equals(parts[0]) ? primary : primary.and(Sort.by("frameId"));
    }
}
