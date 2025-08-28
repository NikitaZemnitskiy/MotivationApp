package com.buseiny.app.model;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OneTimeGoal {
    private String id;
    private String title;
    private int reward;
    private LocalDateTime completedAt; // null если не выполнено

    public OneTimeGoal() {}
    public OneTimeGoal(String id, String title, int reward) {
        this.id = id; this.title = title; this.reward = reward;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getReward() { return reward; }
    public void setReward(int reward) { this.reward = reward; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public boolean isCompleted(){ return completedAt != null; }
}
