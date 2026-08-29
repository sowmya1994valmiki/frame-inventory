package com.global.ct.frameinventory.repository;

import java.util.List;

import com.global.ct.frameinventory.entity.FrameHistory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FrameHistoryRepository extends JpaRepository<FrameHistory, Long> {

    List<FrameHistory> findByFrameFrameIdOrderByOccurredAtDesc(String frameId);
}
