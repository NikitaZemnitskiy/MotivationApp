package com.buseiny.app.model;

/**
 * Admin-defined checkbox daily task with optional streak bonus.
 */
public record GenericDailyTaskDef(
        String id,
        String title,
        int dailyReward,
        boolean streakEnabled
) {}
