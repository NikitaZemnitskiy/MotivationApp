package com.buseiny.app.dto;

/**
 * Weekly requirement progress for a daily task.
 */
public record WeeklyTaskDTO(
        String id,
        String title,
        int required,
        int done
) {}

