package com.buseiny.app.model;

import java.util.*;

import lombok.Data;

@Data
public class UserState {
    private String username; // "Anna"
    private int balance;
    private String avatarUrl;

    // логи по датам (yyyy-MM-dd)
    private Map<String, DailyLog> daily = new HashMap<>();

    // стрики по фиксированным задачам
    private int sportStreak;
    private int englishStreak;
    private int vietWordsStreak;

    // стрики по админским generic задачам: id -> streakCount
    private Map<String, Integer> genericStreaks = new HashMap<>();
    // отметки о выполнении generic задач за дату: yyyy-MM-dd -> set of ids
    private Map<String, Set<String>> genericDoneByDay = new HashMap<>();

    // покупки и достижения
    private List<com.buseiny.app.model.Purchase> purchases = new ArrayList<>();

    // результат рулетки на сегодня
    private RouletteState todayRoulette;
}
