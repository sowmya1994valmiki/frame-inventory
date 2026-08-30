package com.global.ct.frameinventory.repository;

import java.util.Optional;

import com.global.ct.frameinventory.entity.Frame;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FrameRepository extends JpaRepository<Frame, String>, JpaSpecificationExecutor<Frame> {

    Optional<Frame> findOneByFrameId(String frameId);
}
