package com.global.ct.frameinventory.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.global.ct.frameinventory.dto.CreateFrameRequest;
import com.global.ct.frameinventory.dto.LocationDto;
import com.global.ct.frameinventory.dto.SiteDto;
import com.global.ct.frameinventory.entity.Frame;
import com.global.ct.frameinventory.exception.DuplicateFrameException;
import com.global.ct.frameinventory.mapper.FrameMapper;
import com.global.ct.frameinventory.repository.FrameRepository;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.EntityManager;

class FrameServiceTest {

    private final FrameRepository repository = mock(FrameRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final FrameService service = new FrameService(
        repository,
        new FrameMapper(),
        Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneOffset.UTC),
        entityManager
    );

    @Test
    void translatesConcurrentMariaDbDuplicateKeyFailureToConflictException() {
        CreateFrameRequest request = minimalRequest("concurrent");
        DataIntegrityViolationException failure = new DataIntegrityViolationException(
            "duplicate key",
            new SQLException("Duplicate entry", "23000", 1062)
        );
        when(repository.existsById("concurrent")).thenReturn(false);
        when(repository.saveAndFlush(any(Frame.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.createFrame(request))
            .isInstanceOf(DuplicateFrameException.class)
            .hasMessage("Frame 'concurrent' already exists");
    }

    @Test
    void preservesNonDuplicateDataIntegrityFailure() {
        CreateFrameRequest request = minimalRequest("invalid-data");
        DataIntegrityViolationException failure = new DataIntegrityViolationException(
            "value too long",
            new SQLException("Value too long", "22001", 0)
        );
        when(repository.existsById("invalid-data")).thenReturn(false);
        when(repository.saveAndFlush(any(Frame.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.createFrame(request))
            .isSameAs(failure);
    }

    private CreateFrameRequest minimalRequest(String frameId) {
        return new CreateFrameRequest(
            frameId, "DIGITAL", "D6", "UNDERGROUND", "LIVE", null,
            new LocationDto(
                "W1J 9DZ", null, null, null, null, null, "London", null, "London West End",
                null, null, null, null, null
            ),
            new SiteDto("SITE-1", null, null, null, null),
            null, null, null
        );
    }
}
