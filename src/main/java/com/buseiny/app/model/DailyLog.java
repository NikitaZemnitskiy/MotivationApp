package com.buseiny.app.model;

import lombok.Data;

@Data
public class DailyLog {
    // минуты (за день)
    private int nutritionMinutes;
    private int englishMinutes;
    private boolean nutritionDailyAwarded;
    private boolean englishDailyAwarded;
    private boolean sportAwarded;
    private boolean yogaAwarded;
    private boolean vietWordsAwarded;
}
