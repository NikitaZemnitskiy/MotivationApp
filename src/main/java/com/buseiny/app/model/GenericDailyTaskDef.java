package com.buseiny.app.model;

import lombok.Data;

// Простая админская ежедневная задача-чекбокс с опциональным стриком
@Data
public class GenericDailyTaskDef {
    private String id;
    private String title;
    private int dailyReward; // +сколько Бусёинов за день
    private boolean streakEnabled; // если true — каждые 7 подряд +7

    public GenericDailyTaskDef(){}
    public GenericDailyTaskDef(String id, String title, int dailyReward, boolean streakEnabled){
        this.id=id; this.title=title; this.dailyReward=dailyReward; this.streakEnabled=streakEnabled;
    }
}
