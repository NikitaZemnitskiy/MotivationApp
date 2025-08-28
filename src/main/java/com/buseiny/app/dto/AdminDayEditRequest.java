package com.buseiny.app.dto;

import java.util.Set;

public class AdminDayEditRequest {
    public String date;                  // yyyy-MM-dd (обязательное)
    public Integer nutritionMinutes;     // если null — не менять
    public Integer englishMinutes;       // если null — не менять
    public Boolean sportDone;            // если null — не менять
    public Boolean yogaDone;             // если null — не менять
    public Boolean vietDone;             // если null — не менять
    public Set<String> genericDoneIds;   // полный набор id generic-задач, выполненных в этот день (null — не менять)
}