package com.buseiny.app.dto;

import java.util.Set;

/**
 * Admin payload for editing a day's log. Fields set to {@code null} are left unchanged.
 */
public record AdminDayEditRequest(
        String date,
        Integer nutritionMinutes,
        Integer englishMinutes,
        Boolean sportDone,
        Boolean yogaDone,
        Boolean vietDone,
        Set<String> genericDoneIds
) {}
