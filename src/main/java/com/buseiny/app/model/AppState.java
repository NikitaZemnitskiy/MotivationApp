package com.buseiny.app.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import lombok.Data;

@Data
public class AppState {
    private LocalDateTime installedAt;
    private LocalDate lastProcessedWeekStart; // понедельник локальной зоны

    private UserState anna = new UserState();

    private List<OneTimeGoal> goals = new ArrayList<>();
    private List<ShopItem> shop = new ArrayList<>();
    private List<GenericDailyTaskDef> genericDaily = new ArrayList<>();
}
