package com.buseiny.app.dto;

import com.buseiny.app.model.RouletteEffect;

/**
 * Response object describing today's roulette state.
 */
public record RouletteDTO(
        String date,
        RouletteEffect effect,
        String dailyId,
        Integer dailyBaseReward,
        String goalId,
        Integer bonusPoints,
        String discountedShopId,
        String freeShopId,
        boolean canSpin,
        String message,
        String nextSpinAt
) {}
