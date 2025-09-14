package com.buseiny.app.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Admin-defined checkbox daily task with optional streak bonus.
 */
public record GenericDailyTaskDef(
        String id,
        String title,
        int dailyReward,
        boolean streakEnabled,
        int weeklyMin
) {
    @JsonCreator
    public static GenericDailyTaskDef create(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("dailyReward") int dailyReward,
            @JsonProperty("streakEnabled") boolean streakEnabled,
            @JsonProperty("weeklyMin") Integer weeklyMin
    ) {
        return new GenericDailyTaskDef(id, title, dailyReward, streakEnabled,
                weeklyMin == null || weeklyMin <= 0 ? 1 : weeklyMin);
    }
}

