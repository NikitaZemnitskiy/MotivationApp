package com.buseiny.app.model;

// Простая админская ежедневная задача-чекбокс с опциональным стриком
public class GenericDailyTaskDef {
    private String id;
    private String title;
    private int dailyReward; // +сколько Бусёинов за день
    private boolean streakEnabled; // если true — каждые 7 подряд +7

    public GenericDailyTaskDef(){}
    public GenericDailyTaskDef(String id, String title, int dailyReward, boolean streakEnabled){
        this.id=id; this.title=title; this.dailyReward=dailyReward; this.streakEnabled=streakEnabled;
    }
    public String getId(){ return id; }
    public void setId(String id){ this.id=id; }
    public String getTitle(){ return title; }
    public void setTitle(String title){ this.title=title; }
    public int getDailyReward(){ return dailyReward; }
    public void setDailyReward(int dailyReward){ this.dailyReward=dailyReward; }
    public boolean isStreakEnabled(){ return streakEnabled; }
    public void setStreakEnabled(boolean streakEnabled){ this.streakEnabled=streakEnabled; }
}
