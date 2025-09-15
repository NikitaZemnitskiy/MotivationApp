package com.buseiny.app.model;

import lombok.Data;

@Data
public class DailyLog {
    private java.util.Map<String,Integer> minutes = new java.util.HashMap<>();
    private java.util.Set<String> checks = new java.util.HashSet<>();
    private java.util.Set<String> minutesAwarded = new java.util.HashSet<>();
}
