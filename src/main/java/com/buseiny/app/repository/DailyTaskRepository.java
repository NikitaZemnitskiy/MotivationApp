package com.buseiny.app.repository;

import com.buseiny.app.model.DailyTask;
import com.buseiny.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyTaskRepository extends JpaRepository<DailyTask, Long> {
    List<DailyTask> findByUser(User user);
}
