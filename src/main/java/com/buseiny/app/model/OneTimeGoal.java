package com.buseiny.app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OneTimeGoal(
        String id,
        String title,
        int reward,
        LocalDateTime completedAt
) {
    public OneTimeGoal(String id, String title, int reward) {
        this(id, title, reward, null);
    }

    public boolean isCompleted() {
        return completedAt != null;
    }
}
