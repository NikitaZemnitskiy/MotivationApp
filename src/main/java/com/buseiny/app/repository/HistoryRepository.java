package com.buseiny.app.repository;

import com.buseiny.app.model.History;
import com.buseiny.app.model.HistoryType;
import com.buseiny.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {
    List<History> findByUserOrderByTimestampDesc(User user);
    boolean existsByUserAndTypeAndDateAndReason(User user, HistoryType type, LocalDate date, String description);
}
