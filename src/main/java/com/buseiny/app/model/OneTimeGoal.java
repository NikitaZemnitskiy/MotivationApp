package com.buseiny.app.model;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class OneTimeGoal {
    private String id;
    private String title;
    private int reward;
    private LocalDateTime completedAt; // null если не выполнено

    public OneTimeGoal() {}
    public OneTimeGoal(String id, String title, int reward) {
        this.id = id; this.title = title; this.reward = reward;
    }

    public boolean isCompleted(){ return completedAt != null; }
}
