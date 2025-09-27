package com.buseiny.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.scheduling.config.Task;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "history")
@Data
public class History {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int amount; // Положительное = награда, отрицательное = штраф
    private String reason; // Название задания или описание действия
    private LocalDateTime timestamp = LocalDateTime.now(); // Когда произошло событие
    private boolean isDaily; // true если ежедневное задание, false если глобальное или другое
    private LocalDate date = LocalDate.now();

    @ManyToOne
    @JoinColumn(name = "task_id") // связь с таской
    private DailyTask task;

    @Enumerated(EnumType.STRING)
    private HistoryType type;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}

