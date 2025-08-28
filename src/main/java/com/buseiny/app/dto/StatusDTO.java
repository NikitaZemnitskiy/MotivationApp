package com.buseiny.app.dto;

import com.buseiny.app.model.GenericDailyTaskDef;
import com.buseiny.app.model.OneTimeGoal;
import com.buseiny.app.model.ShopItem;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class StatusDTO {
    public String username;
    public String avatarUrl;
    public int balance;

    public int todayNutritionMinutes;
    public boolean todayNutritionAwarded;
    public int todayEnglishMinutes;
    public boolean todayEnglishAwarded;

    public boolean todaySportDone;
    public int sportStreak;

    public boolean todayYogaDone;

    public boolean todayVietDone;
    public int vietStreak;

    public int englishStreak;

    public int weekNutritionMinutes;
    public int weekGoalMinutes; // 1080
    public long secondsUntilWeekEndEpoch; // epoch seconds at week end
    public String currentWeekStart; // yyyy-MM-dd

    public List<OneTimeGoal> goals;
    public List<ShopItem> shop;
    public List<GenericDailyTaskDef> genericDaily;

    public Map<String, Boolean> todayGenericDone; // id -> done?
    public Map<String, Integer> genericStreaks;
}
