package com.buseiny.app.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class AppState {
    private LocalDateTime installedAt;
    private LocalDate lastProcessedWeekStart; // понедельник локальной зоны

    private UserState anna = new UserState();

    private List<OneTimeGoal> goals = new ArrayList<>();
    private List<ShopItem> shop = new ArrayList<>();
    private List<GenericDailyTaskDef> genericDaily = new ArrayList<>();

    public LocalDateTime getInstalledAt(){ return installedAt; }
    public void setInstalledAt(LocalDateTime installedAt){ this.installedAt=installedAt; }
    public LocalDate getLastProcessedWeekStart(){ return lastProcessedWeekStart; }
    public void setLastProcessedWeekStart(LocalDate v){ this.lastProcessedWeekStart=v; }

    public UserState getAnna(){ return anna; }
    public void setAnna(UserState anna){ this.anna=anna; }

    public List<OneTimeGoal> getGoals(){ return goals; }
    public void setGoals(List<OneTimeGoal> goals){ this.goals=goals; }
    public List<ShopItem> getShop(){ return shop; }
    public void setShop(List<ShopItem> shop){ this.shop=shop; }
    public List<GenericDailyTaskDef> getGenericDaily(){ return genericDaily; }
    public void setGenericDaily(List<GenericDailyTaskDef> genericDaily){ this.genericDaily=genericDaily; }
}
