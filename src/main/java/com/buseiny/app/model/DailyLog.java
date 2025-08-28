package com.buseiny.app.model;

public class DailyLog {
    // минуты (за день)
    private int nutritionMinutes;
    private int englishMinutes;
    // флаги, что дневная награда уже выдана
    private boolean nutritionDailyAwarded;
    private boolean englishDailyAwarded;
    private boolean sportAwarded;
    private boolean yogaAwarded;
    private boolean vietWordsAwarded;

    public int getNutritionMinutes(){ return nutritionMinutes; }
    public void setNutritionMinutes(int v){ nutritionMinutes=v; }
    public int getEnglishMinutes(){ return englishMinutes; }
    public void setEnglishMinutes(int v){ englishMinutes=v; }
    public boolean isNutritionDailyAwarded(){ return nutritionDailyAwarded; }
    public void setNutritionDailyAwarded(boolean v){ nutritionDailyAwarded=v; }
    public boolean isEnglishDailyAwarded(){ return englishDailyAwarded; }
    public void setEnglishDailyAwarded(boolean v){ englishDailyAwarded=v; }
    public boolean isSportAwarded(){ return sportAwarded; }
    public void setSportAwarded(boolean v){ sportAwarded=v; }
    public boolean isYogaAwarded(){ return yogaAwarded; }
    public void setYogaAwarded(boolean v){ yogaAwarded=v; }
    public boolean isVietWordsAwarded(){ return vietWordsAwarded; }
    public void setVietWordsAwarded(boolean v){ vietWordsAwarded=v; }
}
