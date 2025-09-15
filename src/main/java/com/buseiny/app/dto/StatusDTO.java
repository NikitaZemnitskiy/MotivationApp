package com.buseiny.app.dto;

import com.buseiny.app.model.OneTimeGoal;
import com.buseiny.app.model.ShopItem;
import java.util.List;
import java.util.Map;

public record StatusDTO(
        String username,
        String avatarUrl,
        int balance,
        int weekMinutes,
        int weekGoalMinutes,
        long secondsUntilWeekEndEpoch,
        String currentWeekStart,
        List<OneTimeGoal> goals,
        List<ShopItem> shop,
        List<Map<String,Object>> tasks,
        List<WeeklyTaskDTO> weekDaily,
        List<?> gifts
) {}
