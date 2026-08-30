package com.global.ct.frameinventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import com.global.ct.frameinventory.dto.CreateFrameRequest;
import com.global.ct.frameinventory.dto.LocationDto;
import com.global.ct.frameinventory.dto.SiteDto;
import com.global.ct.frameinventory.dto.UpdateFrameRequest;
import com.global.ct.frameinventory.entity.Frame;
import com.global.ct.frameinventory.entity.FrameHistory;
import com.global.ct.frameinventory.exception.DuplicateFrameException;
import com.global.ct.frameinventory.mapper.FrameMapper;
import com.global.ct.frameinventory.repository.FrameHistoryRepository;
import com.global.ct.frameinventory.repository.FrameRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.EntityManager;

@ExtendWith(OutputCaptureExtension.class)
class FrameServiceTest {

    private final FrameRepository repository = mock(FrameRepository.class);
    private final FrameHistoryRepository historyRepository = mock(FrameHistoryRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final FrameService service = new FrameService(
        repository,
        historyRepository,
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

    @Test
    void noOpUpdateDoesNotSaveFrameOrHistory() {
        CreateFrameRequest createRequest = minimalRequest("unchanged");
        Frame existing = new FrameMapper().toNewEntity(
            createRequest,
            LocalDateTime.of(2026, 8, 29, 11, 0)
        );
        UpdateFrameRequest updateRequest = new UpdateFrameRequest(
            createRequest.mediaType(), createRequest.format(), createRequest.environment(),
            createRequest.status(), createRequest.statusReason(), createRequest.location(),
            createRequest.site(), createRequest.technical(), createRequest.commercial(),
            createRequest.integrations()
        );
        when(repository.findOneByFrameId("unchanged")).thenReturn(Optional.of(existing));

        var response = service.replaceFrame("unchanged", updateRequest);

        assertThat(response.modifiedDate()).isEqualTo(Instant.parse("2026-08-29T11:00:00Z"));
        verify(repository).findOneByFrameId("unchanged");
        verify(repository, never()).saveAndFlush(any(Frame.class));
        verify(historyRepository, never()).save(any(FrameHistory.class));
    }

    @Test
    void logsSuccessfulManualCreationWithSanitizedFrameId(CapturedOutput output) {
        CreateFrameRequest request = minimalRequest("created\nframe");
        when(repository.existsById(request.frameId())).thenReturn(false);
        when(repository.saveAndFlush(any(Frame.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service.createFrame(request);

        assertThat(output).contains("Frame created frameId=created_frame");
    }

    @Test
    void doesNotLogEachImportedFrame(CapturedOutput output) {
        CreateFrameRequest request = minimalRequest("imported-frame");
        when(repository.existsById(request.frameId())).thenReturn(false);
        when(repository.saveAndFlush(any(Frame.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service.createImportedFrame(request);

        assertThat(output).doesNotContain("Frame created frameId=imported-frame");
    }

    @Test
    void noOpUpdateDoesNotProduceOperationalLog(CapturedOutput output) {
        CreateFrameRequest createRequest = minimalRequest("unchanged-log");
        Frame existing = new FrameMapper().toNewEntity(
            createRequest,
            LocalDateTime.of(2026, 8, 29, 11, 0)
        );
        UpdateFrameRequest updateRequest = new UpdateFrameRequest(
            createRequest.mediaType(), createRequest.format(), createRequest.environment(),
            createRequest.status(), createRequest.statusReason(), createRequest.location(),
            createRequest.site(), createRequest.technical(), createRequest.commercial(),
            createRequest.integrations()
        );
        when(repository.findOneByFrameId("unchanged-log")).thenReturn(Optional.of(existing));

        service.replaceFrame("unchanged-log", updateRequest);

        assertThat(output).doesNotContain("Frame updated frameId=unchanged-log");
    }

    @Test
    void getReturnsStoredFrameIdCasingForCaseInsensitiveLookup() {
        CreateFrameRequest createRequest = minimalRequest("Stored-Case");
        Frame existing = new FrameMapper().toNewEntity(
            createRequest,
            LocalDateTime.of(2026, 8, 29, 11, 0)
        );
        when(repository.findOneByFrameId("stored-case")).thenReturn(Optional.of(existing));

        var response = service.getFrame("stored-case");

        assertThat(response.frameId()).isEqualTo("Stored-Case");
        verify(repository).findOneByFrameId("stored-case");
    }

    @Test
    void updatePreservesStoredFrameIdCasingAndLogsChangeCount(CapturedOutput output) {
        CreateFrameRequest createRequest = minimalRequest("Stored-\nCase");
        Frame existing = new FrameMapper().toNewEntity(
            createRequest,
            LocalDateTime.of(2026, 8, 29, 11, 0)
        );
        UpdateFrameRequest updateRequest = new UpdateFrameRequest(
            createRequest.mediaType(), createRequest.format(), createRequest.environment(),
            "INACTIVE", null, createRequest.location(), createRequest.site(),
            createRequest.technical(), createRequest.commercial(), createRequest.integrations()
        );
        when(repository.findOneByFrameId("stored-case")).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any(Frame.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.replaceFrame("stored-case", updateRequest);

        assertThat(response.frameId()).isEqualTo("Stored-\nCase");
        assertThat(response.status()).isEqualTo("INACTIVE");
        assertThat(output).contains("Frame updated frameId=Stored-_Case changedFields=1");
        verify(repository).findOneByFrameId("stored-case");
        verify(historyRepository).save(any(FrameHistory.class));
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
