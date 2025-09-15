package com.buseiny.app.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Unified daily task definition.
 * MINUTES tasks track minutes per day and are considered completed when minutes >= minutesPerDay.
 * CHECK tasks are boolean check-offs for the day.
 */
public record DailyTaskDef(
        String id,
        String title,
        DailyTaskKind kind,
        int dailyReward,
        Integer minutesPerDay,
        Integer weeklyMinutesGoal,
        boolean streakEnabled,
        Integer weeklyRequiredCount
) {
    @JsonCreator
    public static DailyTaskDef create(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("kind") DailyTaskKind kind,
            @JsonProperty("dailyReward") Integer dailyReward,
            @JsonProperty("minutesPerDay") Integer minutesPerDay,
            @JsonProperty("weeklyMinutesGoal") Integer weeklyMinutesGoal,
            @JsonProperty("streakEnabled") Boolean streakEnabled,
            @JsonProperty("weeklyRequiredCount") Integer weeklyRequiredCount
    ) {
        return new DailyTaskDef(
                id,
                title,
                kind,
                dailyReward == null ? 1 : dailyReward,
                minutesPerDay,
                weeklyMinutesGoal,
                streakEnabled != null && streakEnabled,
                weeklyRequiredCount == null || weeklyRequiredCount <= 0 ? 1 : weeklyRequiredCount
        );
    }
}


