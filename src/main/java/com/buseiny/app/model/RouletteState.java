package com.buseiny.app.model;

import java.time.LocalDate;

import lombok.Data;

@Data
public class RouletteState {
    private LocalDate date;              // на какой день выдан результат
    private RouletteEffect effect;

    // Для DAILY_X2
    private String dailyId;              // id выбранного дейлика
    private Integer dailyBaseReward;     // базовая награда
    private boolean dailyPenaltyApplied; // штраф уже применён?

    // Для GOAL_X2
    private String goalId;

    // Для BONUS_POINTS
    private Integer bonusPoints;         // 1..5

    // Для магазинов
    private String discountedShopId;     // -50% на itemId
    private String freeShopId;           // бесплатный itemId (<100)
}

