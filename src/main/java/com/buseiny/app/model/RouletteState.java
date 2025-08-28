package com.buseiny.app.model;

import java.time.LocalDate;

import lombok.Data;

@Data
public class RouletteState {
    private LocalDate date;              // day the result applies to
    private RouletteEffect effect;

    // For DAILY_X2
    private String dailyId;              // chosen daily id
    private Integer dailyBaseReward;     // base reward
    private boolean dailyPenaltyApplied; // penalty applied?

    // For GOAL_X2
    private String goalId;

    // For BONUS_POINTS
    private Integer bonusPoints;         // 1..5

    // For shop rewards
    private String discountedShopId;     // 50% off item id
    private String freeShopId;           // free item id (<100)
}

