package com.buseiny.app.dto;

import com.buseiny.app.model.GenericDailyTaskDef;
import com.buseiny.app.model.OneTimeGoal;
import com.buseiny.app.model.ShopItem;

import java.util.List;
import java.util.Map;

/**
 * Snapshot of the current user status returned by API.
 */
public record StatusDTO(
        String username,
        String avatarUrl,
        int balance,

        int todayNutritionMinutes,
        boolean todayNutritionAwarded,
        int todayEnglishMinutes,
        boolean todayEnglishAwarded,

        boolean todaySportDone,
        int sportStreak,

        boolean todayYogaDone,

        boolean todayVietDone,
        int vietStreak,

        int englishStreak,

        int weekNutritionMinutes,
        int weekGoalMinutes,
        long secondsUntilWeekEndEpoch,
        String currentWeekStart,

        List<OneTimeGoal> goals,
        List<ShopItem> shop,
        List<GenericDailyTaskDef> genericDaily,

        Map<String, Boolean> todayGenericDone,
        Map<String, Integer> genericStreaks
) {}
