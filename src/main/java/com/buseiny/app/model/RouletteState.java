package com.buseiny.app.model;

import java.time.LocalDate;

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

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public RouletteEffect getEffect() { return effect; }
    public void setEffect(RouletteEffect effect) { this.effect = effect; }

    public String getDailyId() { return dailyId; }
    public void setDailyId(String dailyId) { this.dailyId = dailyId; }

    public Integer getDailyBaseReward() { return dailyBaseReward; }
    public void setDailyBaseReward(Integer dailyBaseReward) { this.dailyBaseReward = dailyBaseReward; }

    public boolean isDailyPenaltyApplied() { return dailyPenaltyApplied; }
    public void setDailyPenaltyApplied(boolean dailyPenaltyApplied) { this.dailyPenaltyApplied = dailyPenaltyApplied; }

    public String getGoalId() { return goalId; }
    public void setGoalId(String goalId) { this.goalId = goalId; }

    public Integer getBonusPoints() { return bonusPoints; }
    public void setBonusPoints(Integer bonusPoints) { this.bonusPoints = bonusPoints; }

    public String getDiscountedShopId() { return discountedShopId; }
    public void setDiscountedShopId(String discountedShopId) { this.discountedShopId = discountedShopId; }

    public String getFreeShopId() { return freeShopId; }
    public void setFreeShopId(String freeShopId) { this.freeShopId = freeShopId; }
}

