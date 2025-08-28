package com.buseiny.app.model;

import java.util.*;

public class UserState {
    private String username; // "Anna"
    private int balance;
    private String avatarUrl;

    // логи по датам (yyyy-MM-dd)
    private Map<String, DailyLog> daily = new HashMap<>();

    // стрики по фиксированным задачам
    private int sportStreak;
    private int englishStreak;
    private int vietWordsStreak;

    // стрики по админским generic задачам: id -> streakCount
    private Map<String, Integer> genericStreaks = new HashMap<>();
    // отметки о выполнении generic задач за дату: yyyy-MM-dd -> set of ids
    private Map<String, Set<String>> genericDoneByDay = new HashMap<>();

    // покупки и достижения
    private List<com.buseiny.app.model.Purchase> purchases = new ArrayList<>();

    public String getUsername(){ return username; }
    public void setUsername(String username){ this.username=username; }
    public int getBalance(){ return balance; }
    public void setBalance(int balance){ this.balance=balance; }
    public String getAvatarUrl(){ return avatarUrl; }
    public void setAvatarUrl(String avatarUrl){ this.avatarUrl=avatarUrl; }
    public Map<String, DailyLog> getDaily(){ return daily; }
    public void setDaily(Map<String, DailyLog> daily){ this.daily=daily; }
    public int getSportStreak(){ return sportStreak; }
    public void setSportStreak(int sportStreak){ this.sportStreak=sportStreak; }
    public int getEnglishStreak(){ return englishStreak; }
    public void setEnglishStreak(int englishStreak){ this.englishStreak=englishStreak; }
    public int getVietWordsStreak(){ return vietWordsStreak; }
    public void setVietWordsStreak(int vietWordsStreak){ this.vietWordsStreak=vietWordsStreak; }
    public Map<String, Integer> getGenericStreaks(){ return genericStreaks; }
    public void setGenericStreaks(Map<String,Integer> genericStreaks){ this.genericStreaks=genericStreaks; }
    public Map<String, Set<String>> getGenericDoneByDay(){ return genericDoneByDay; }
    public void setGenericDoneByDay(Map<String, Set<String>> v){ this.genericDoneByDay=v; }
    public List<Purchase> getPurchases(){ return purchases; }
    public void setPurchases(List<Purchase> purchases){ this.purchases=purchases; }

    private java.util.Map<String, RouletteEffect> rouletteByDay = new java.util.HashMap<>();
    public java.util.Map<String, RouletteEffect> getRouletteByDay() { return rouletteByDay; }
    public void setRouletteByDay(java.util.Map<String, RouletteEffect> m){ this.rouletteByDay = m; }
}
