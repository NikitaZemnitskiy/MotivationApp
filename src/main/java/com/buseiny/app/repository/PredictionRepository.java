package com.buseiny.app.repository;

import com.buseiny.app.model.PredictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PredictionRepository extends JpaRepository<PredictionEntity, Long> {
    PredictionEntity findTopByUser_UsernameAndResolvedFalseOrderByCreatedAtDesc(String username);
}

