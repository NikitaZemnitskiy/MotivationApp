package com.buseiny.app.model;

import java.util.*;

import lombok.Data;

@Data
public class UserState {
    private String username; // default username
    private int balance;
    private String avatarUrl;

    // logs by date (yyyy-MM-dd)
    private Map<String, DailyLog> daily = new HashMap<>();

    // streaks for fixed tasks
    private int sportStreak;
    private int englishStreak;
    private int vietWordsStreak;

    // streaks for admin generic tasks: id -> streak count
    private Map<String, Integer> genericStreaks = new HashMap<>();
    // records of generic tasks per date: yyyy-MM-dd -> set of ids
    private Map<String, Set<String>> genericDoneByDay = new HashMap<>();

    // purchases and achievements
    private List<com.buseiny.app.model.Purchase> purchases = new ArrayList<>();

    // roulette result for today
    private RouletteState todayRoulette;
}
