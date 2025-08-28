package com.buseiny.app.model;

import java.time.LocalDateTime;

public enum RouletteEffect {
    DAILY_X2,            // Случайный дейлик сегодня x2; если не сделан — штраф на базовую стоимость
    GOAL_X2,             // Случайная разовая цель: сегодня x2
    BONUS_POINTS,        // Немедленно +N (1..5)
    SHOP_DISCOUNT_50,    // -50% на случайный товар (только сегодня)
    SHOP_FREE_UNDER_100  // Случайный товар <100 — бесплатен (только сегодня)
}
