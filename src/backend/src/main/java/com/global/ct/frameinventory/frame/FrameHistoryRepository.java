package com.global.ct.frameinventory.frame;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FrameHistoryRepository extends JpaRepository<FrameHistory, Long> {

    List<FrameHistory> findByFrameFrameIdOrderByOccurredAtDesc(String frameId);
}
