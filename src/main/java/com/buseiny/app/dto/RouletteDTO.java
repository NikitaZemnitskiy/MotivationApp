package com.buseiny.app.dto;

import com.buseiny.app.model.RouletteEffect;

public class RouletteDTO {
    public String date;               // yyyy-MM-dd
    public RouletteEffect effect;
    public String dailyId;
    public Integer dailyBaseReward;
    public String goalId;
    public Integer bonusPoints;
    public String discountedShopId;
    public String freeShopId;
    public boolean canSpin;
    public String message;
    public String nextSpinAt;
}
