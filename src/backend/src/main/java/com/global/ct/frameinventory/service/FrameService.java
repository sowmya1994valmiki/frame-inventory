package com.global.ct.frameinventory.service;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.global.ct.frameinventory.dto.CreateFrameRequest;
import com.global.ct.frameinventory.dto.FramePageResponse;
import com.global.ct.frameinventory.dto.FrameResponse;
import com.global.ct.frameinventory.dto.FrameSearchCriteria;
import com.global.ct.frameinventory.dto.FrameSummaryResponse;
import com.global.ct.frameinventory.dto.UpdateFrameRequest;
import com.global.ct.frameinventory.entity.Frame;
import com.global.ct.frameinventory.exception.DuplicateFrameException;
import com.global.ct.frameinventory.exception.FrameNotFoundException;
import com.global.ct.frameinventory.mapper.FrameMapper;
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
    private final FrameMapper mapper;
    private final Clock clock;
    private final EntityManager entityManager;

    public FrameService(
        FrameRepository repository,
        FrameMapper mapper,
        Clock clock,
        EntityManager entityManager
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Transactional
    public FrameResponse createFrame(CreateFrameRequest request) {
        if (repository.existsById(request.frameId())) {
            throw new DuplicateFrameException(request.frameId());
        }

        Frame frame = mapper.toNewEntity(request, now());
        try {
            Frame saved = repository.saveAndFlush(frame);
            entityManager.refresh(saved);
            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateKey(exception)) {
                throw new DuplicateFrameException(request.frameId());
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public FrameResponse getFrame(String frameId) {
        return repository.findById(frameId)
            .map(mapper::toResponse)
            .orElseThrow(() -> new FrameNotFoundException(frameId));
    }

    @Transactional
    public FrameResponse replaceFrame(String frameId, UpdateFrameRequest request) {
        Frame existing = repository.findById(frameId)
            .orElseThrow(() -> new FrameNotFoundException(frameId));
        Frame replacement = mapper.toReplacement(existing, request, now());
        Frame saved = repository.saveAndFlush(replacement);
        entityManager.refresh(saved);
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

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
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
