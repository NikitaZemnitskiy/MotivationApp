package com.buseiny.app.model;

public enum RouletteEffect {
    DAILY_X2,            // random daily task gives x2 reward today; penalty if missed
    GOAL_X2,             // random one-time goal gives x2 reward today
    BONUS_POINTS,        // immediate bonus points (1..5)
    SHOP_DISCOUNT_50,    // 50% off a random shop item (today only)
    SHOP_FREE_UNDER_100  // random item under 100 is free (today only)
}
