package com.buseiny.app.model;

import lombok.Data;

@Data
public class DailyLog {
    private java.util.Map<String,Integer> minutes = new java.util.HashMap<>();
    private java.util.Set<String> checks = new java.util.HashSet<>();
    // For minutes-type tasks, track which were already awarded today
    private java.util.Set<String> minutesAwarded = new java.util.HashSet<>();
}
