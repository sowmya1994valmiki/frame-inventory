package com.global.ct.frameinventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import com.global.ct.frameinventory.dto.CreateFrameRequest;
import com.global.ct.frameinventory.dto.LocationDto;
import com.global.ct.frameinventory.dto.SiteDto;
import com.global.ct.frameinventory.dto.UpdateFrameRequest;
import com.global.ct.frameinventory.entity.Frame;
import com.global.ct.frameinventory.entity.FrameHistory;
import com.global.ct.frameinventory.mapper.FrameMapper;
import com.global.ct.frameinventory.repository.FrameHistoryRepository;
import com.global.ct.frameinventory.repository.FrameRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class FrameHistoryRollbackIntegrationTest {

    @Autowired
    private FrameService service;

    @Autowired
    private FrameRepository frameRepository;

    @Autowired
    private FrameMapper mapper;

    @MockitoBean
    private FrameHistoryRepository historyRepository;

    @BeforeEach
    void clearFrames() {
        frameRepository.deleteAll();
    }

    @Test
    void rollsBackFrameCreateWhenHistoryPersistenceFails() {
        CreateFrameRequest request = createRequest("create-rollback");
        failHistoryPersistence();

        assertThatThrownBy(() -> service.createFrame(request))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessage("history insert failed");

        assertThat(frameRepository.existsById("create-rollback")).isFalse();
    }

    @Test
    void rollsBackFrameUpdateWhenHistoryPersistenceFails() {
        CreateFrameRequest createRequest = createRequest("update-rollback");
        Frame frame = mapper.toNewEntity(
            createRequest,
            LocalDateTime.of(2026, 8, 30, 9, 0)
        );
        frameRepository.saveAndFlush(frame);
        UpdateFrameRequest updateRequest = new UpdateFrameRequest(
            createRequest.mediaType(), createRequest.format(), createRequest.environment(),
            "INACTIVE", "Maintenance", createRequest.location(), createRequest.site(),
            createRequest.technical(), createRequest.commercial(), createRequest.integrations()
        );
        failHistoryPersistence();

        assertThatThrownBy(() -> service.replaceFrame("update-rollback", updateRequest))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessage("history insert failed");

        Frame unchanged = frameRepository.findById("update-rollback").orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo("LIVE");
        assertThat(unchanged.getStatusReason()).isNull();
        assertThat(unchanged.getModifiedDate()).isEqualTo(LocalDateTime.of(2026, 8, 30, 9, 0));
    }

    private void failHistoryPersistence() {
        when(historyRepository.save(any(FrameHistory.class)))
            .thenThrow(new DataIntegrityViolationException("history insert failed"));
    }

    private CreateFrameRequest createRequest(String frameId) {
        return new CreateFrameRequest(
            frameId, "DIGITAL", "D6", "UNDERGROUND", "LIVE", null,
            new LocationDto(
                "W1J 9DZ", null, null, null, null, null, "London", null,
                "London West End", null, null, null, null, null
            ),
            new SiteDto("SITE-1", null, null, null, null),
            null, null, null
        );
    }
}
