package com.global.ct.frameinventory.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.global.ct.frameinventory.entity.FieldChange;
import com.global.ct.frameinventory.entity.Frame;
import com.global.ct.frameinventory.entity.FrameHistory;
import com.global.ct.frameinventory.entity.HistoryEventType;
import com.global.ct.frameinventory.entity.HistorySource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;

@DataJpaTest
class FrameHistoryRepositoryPersistenceTest {

    @Autowired
    private FrameRepository frameRepository;

    @Autowired
    private FrameHistoryRepository frameHistoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndRetrievesJsonFieldChangesForAFrame() {
        Frame frame = frameRepository.saveAndFlush(
            FrameRepositoryPersistenceTest.completeFrame("1234604983")
        );
        Map<String, FieldChange> changes = new LinkedHashMap<>();
        changes.put("status", new FieldChange("INACTIVE", "LIVE"));
        changes.put(
            "location.postcode",
            new FieldChange("W1J 8DZ", "W1J 9DZ")
        );
        Instant occurredAt = Instant.parse("2026-05-12T09:15:30Z");

        frameHistoryRepository.saveAndFlush(new FrameHistory(
            frame,
            HistoryEventType.UPDATED,
            occurredAt,
            HistorySource.MANUAL,
            changes
        ));
        entityManager.clear();

        List<FrameHistory> history = frameHistoryRepository
            .findByFrameFrameIdOrderByOccurredAtDesc("1234604983");

        assertThat(history).hasSize(1);
        FrameHistory loaded = history.getFirst();
        assertThat(loaded.getId()).isNotNull();
        assertThat(loaded.getFrame().getFrameId()).isEqualTo("1234604983");
        assertThat(loaded.getEventType()).isEqualTo(HistoryEventType.UPDATED);
        assertThat(loaded.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(loaded.getSource()).isEqualTo(HistorySource.MANUAL);
        assertThat(loaded.getChangedFields()).containsExactlyEntriesOf(changes);
    }
}
