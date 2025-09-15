package com.buseiny.app.model;

import java.util.*;

import lombok.Data;
import com.buseiny.app.dto.HistoryDTO;

@Data
public class UserState {
    private String username; // default username
    private int balance;
    private String avatarUrl;

    // logs by date (yyyy-MM-dd)
    private Map<String, DailyLog> daily = new HashMap<>();

    // streaks for tasks: taskId -> streak count
    private Map<String, Integer> streaks = new HashMap<>();

    // extra history entries like roulette bonuses
    private Map<String, List<HistoryDTO.Item>> historyExtras = new HashMap<>();

    // purchases and achievements
    private List<com.buseiny.app.model.Purchase> purchases = new ArrayList<>();

    // pending gifts from admin
    private List<Gift> gifts = new ArrayList<>();

    // roulette result for today
    private RouletteState todayRoulette;
}
