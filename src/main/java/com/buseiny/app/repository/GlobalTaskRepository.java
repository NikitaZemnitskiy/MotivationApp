package com.buseiny.app.repository;

import com.buseiny.app.model.GlobalTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalTaskRepository extends JpaRepository<GlobalTask, Long> { }
